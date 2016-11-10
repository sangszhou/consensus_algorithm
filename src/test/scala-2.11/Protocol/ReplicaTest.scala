//package Protocol
//
//import Protocol.Messages._
//import Protocol.Replica.{LookUpReplicaState, ReplicaState}
//import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
//import akka.testkit.{ImplicitSender, TestKit}
//import org.scalatest._
//
//import scala.concurrent.duration._
//import akka.pattern.ask
//import akka.util.Timeout
//
//import scala.concurrent.Await
///**
//  * Created by xinszhou on 11/9/16.
//  */
//class ReplicaTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//  with WordSpecLike with ShouldMatchers with BeforeAndAfterAll {
//
//  implicit val timtout = Timeout(5 second)
//
//  def this() = this(ActorSystem("ReplicaTestActorSystem"))
//
////  override def afterAll(): Unit = {
////    _system.terminate()
////    super.afterAll()
////  }
//
//  object ListStateReplica {
//    def props = Props(new ListStateReplica)
//  }
//
//  class ListStateReplica extends Replica with ReplicateStateMachine {
//    var state = List.empty[Command]
//    override def execute(cmd: Command) = state = cmd :: state
//    override def internalState: Any = state
//  }
//
//  case object SampleOperation extends Operation
//
//  val replicaActor = _system.actorOf(ListStateReplica.props, "Listable_state_replica_actor")
//
//  "one replica node " must {
//    val clientId = "clientId1"
//    val cmd = Command(clientId, "1", SampleOperation)
//    val requestMessage = RequestMessage(clientId, cmd)
//    val decisionMsg = DecisionMessage("testActor", 1, cmd)
//
//    "receive and execute new command" in {
//
//      // used to update client info
//      replicaActor ! requestMessage
//
//      var replicaState = getReplicaState
//
//      // must be int or long
//      replicaState.clientInfo.get(clientId).isDefined should be (true)
//
//
//      // add one command to replica
//      // expecting response message
//      replicaActor ! decisionMsg
//      expectMsg(3 second, ResponseMessage(clientId))
//
//      replicaState = getReplicaState
//
//      replicaState.slotOut should be (2)
//      replicaState.decision should  contain (1, cmd)
//      replicaState.decision should have size 1
//      replicaState.rsm.asInstanceOf[List[Command]] should be (List(cmd))
//
//    }
//
//    "reject command with same slot number since it's already on decision list" in {
//      replicaActor ! decisionMsg
//      expectNoMsg(3 second)
//
//      val replicaState = getReplicaState
//
//      replicaState.slotOut should be (2)
//      replicaState.decision should have size 1
//
//    }
//
//    "add same command with different slot number and dont notify client" in {
//      val decisionMsg = DecisionMessage("testActor", 2, cmd)
//      replicaActor ! decisionMsg
//
//      expectNoMsg(3 second)
//
//      val replicaState = getReplicaState
//      replicaState.slotOut should be (3)
//      replicaState.decision should contain (2, cmd)
//      replicaState.rsm.asInstanceOf[List[Command]] should be (List(cmd))
//    }
//
//  }
//
//  def getReplicaState: ReplicaState = {
//    Await.result(replicaActor ? LookUpReplicaState, 3 second).asInstanceOf[ReplicaState]
//  }
//
//
//
//
//}
