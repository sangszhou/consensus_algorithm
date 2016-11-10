package Protocol

import Protocol.Messages._
import Protocol.Utils.ListStateReplica
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{ShouldMatchers, WordSpecLike}

import scala.util.Random

/**
  * Created by xinszhou on 11/10/16.
  */
class HeavyTest(_system: ActorSystem) extends TestKit(_system) with WordSpecLike
  with ShouldMatchers with ImplicitSender {

  def this() = this(ActorSystem("Paxos-spec-actor-system"))

  val availableOps = List(op1, op2, op3, op4)

  val num = 100
  val messages: List[RequestMessage] = {
    (1 to num) map(i => {
      val index = Random.nextInt(4)
      val op = availableOps.apply(index)
      RequestMessage(Command("client" + index/10, i.toString, op))
    }) toList
  }

  "send 100 messages to replica and wait response" must {

    "send randomly to 2 replicas" in {

      val acceptor1 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-1")
      val acceptor2 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-2")
      val acceptor3 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-3")

      val replica1: ActorRef  = _system.actorOf(ListStateReplica.props(2), "replica-1")
      val replica2: ActorRef  = _system.actorOf(ListStateReplica.props(2), "replica-2")

      val tempLeader1 = _system.actorOf(Leader.props(Set(acceptor1, acceptor2, acceptor3),
        Set(replica1, replica2)), "leader-1")
      val tempLeader2 = _system.actorOf(Leader.props(Set(acceptor1, acceptor2, acceptor3),
        Set(replica1, replica2)), "leader-2")

      Thread.sleep(1000)

      messages.foreach(request => {
        if(Random.nextInt(2) == 0) {
          replica1 ! request
        } else {
          replica2 ! request
        }
      })

      Thread.sleep(3000)

      val replicaState1 = Replica.getInternalState(replica1)
      val replicaState2 = Replica.getInternalState(replica2)

      println(s"replica decision is ${replicaState1.decision}")
      println(s"replica slot out is ${replicaState1.slotOut}")

      replicaState1.decision should be (replicaState2.decision)

      // could return more, but never less
      replicaState1.decision.values.map(_.reqId.toInt).toSet should have size 100
    }

//    "send all messages to 2 replicas" in {
//
//    }

  }

}
