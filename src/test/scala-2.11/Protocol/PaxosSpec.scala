package Protocol

import Protocol.Messages.{BallotNumber, Command, ProposalMessage, TestOperation}
import Protocol.Utils.ListStateReplica
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{ShouldMatchers, WordSpecLike}
import Messages._
/**
  * Created by xinszhou on 11/10/16.
  */
class PaxosSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with ShouldMatchers with WordSpecLike {

  def this() = this(ActorSystem("Paxos-spec-actor-system"))

  /**
    * send two requests with same slot to two different leaders, and read the outcome:
    * no matter which request is chosen, the replicas state machine remain same
    */
  "send two messages to two leaders" must {

    val acceptor1 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-1")
    val acceptor2 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-2")
    val acceptor3 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-3")

    var replica1: ActorRef = null
    var replica2: ActorRef = null

    replica1 = _system.actorOf(ListStateReplica.props(2), "replica-1")
    replica2 = _system.actorOf(ListStateReplica.props(2), "replica-2")

    var tempLeader1: ActorRef = null

    var tempLeader2: ActorRef = null

    val cmd1 = Command("client1", "reqId1", TestOperation)
    val cmd2 = Command("client2", "reqId2", TestOperation)
    val cmd3 = Command("client3", "reqId3", TestOperation)
    val cmd4 = Command("client4", "reqId3", TestOperation)

    "if conflict happened, cluster" must {

      "start two leader one after another" in {
        tempLeader1 = _system.actorOf(Leader.props(Set(acceptor1, acceptor2, acceptor3),
          Set(replica1, replica2)), "leader-1")

        Thread.sleep(300)

        val acceptor1State = Acceptor.getAcceptorStatus(acceptor1)
        acceptor1State.maxBallotNumSeenSoFar should be (new BallotNumber("leader-1", 1))



        tempLeader2 = _system.actorOf(Leader.props(Set(acceptor1, acceptor2, acceptor3),
          Set(replica1, replica2)), "leader-2")

        // may error if not sleep
        Thread.sleep(300)

        val acceptor2State = Acceptor.getAcceptorStatus(acceptor2)
        acceptor2State.maxBallotNumSeenSoFar should be (new BallotNumber("leader-2", 1))

      }

      "two leader become stable after several round competing for view, at least one is active" in {
        val leader1State = Leader.getLeaderStatus(tempLeader1)
        val leader2State = Leader.getLeaderStatus(tempLeader2)

        (leader1State.active || leader2State.active) should be (true)

      }
//      @todo open this test case if something is wrong. this is a more fine grained test case
//      "leader2 in charge now, send leader1 proposal message will force leader1 to regain control" in {
//        tempLeader1 ! ProposalMessage(1, cmd1)
//
//        Thread.sleep(300)
//
//        val acceptor1State = Acceptor.getAcceptorStatus(acceptor1)
//        acceptor1State.maxBallotNumSeenSoFar should be (new BallotNumber("leader-1", 2))
//
//      }

      /**
        * 这个场景非常复杂
        * 1. 假如 leader1 完成所有操作后, leader 的请求才发出, 那么 leader2 会抛掉自己的 proposal, 因为
        * 他会从 acceptor 中学到 slot1 对应的值
        * 2. 假如 leader1 完成 phase1, 正要发送时, leader2 抢占了 view, 然后进行自己的操作, 那么 leader2 成功
        * 总而言之, 最后只能有一个值被确定下来, 这个值和 replica 上的值是一样的
        */
      "replica receive only one decision, if send directly to " in {
        tempLeader1 ! ProposalMessage(1, cmd1)
        tempLeader2 ! ProposalMessage(1, cmd2)

        Thread.sleep(300) // wait a little longer

        val replicaState1 = Replica.getInternalState(replica1)
        val replicaState2 = Replica.getInternalState(replica2)

        replicaState1.decision should have size (1)
        replicaState2.decision should have size (1)

        replicaState1.decision.get(1) match {
          case Some(cmd1) =>
            println("cmd1 wins")
            replicaState2.decision should contain (1 -> cmd1)
          case Some(cmd2) =>
            println("cmd2 wins")
            replicaState2.decision should contain (1 -> cmd2)
        }

      }

      "send one message to replica and two replicas should see some result" in {

        replica1 ! RequestMessage(cmd3)

        Thread.sleep(1000)

        val replicaState1 = Replica.getInternalState(replica1)
        val replicaState2 = Replica.getInternalState(replica2)

        replicaState1.decision should contain (2 -> cmd3)

        replicaState1.decision should be (replicaState2.decision)

      }

    }
  }




}
