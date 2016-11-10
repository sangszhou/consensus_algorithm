//package Protocol
//
//import Protocol.Messages._
//import Protocol.Utils.ListStateReplica
//import akka.actor.{ActorSystem, Props}
//import akka.testkit.{ImplicitSender, TestKit}
//import org.scalatest.{FunSuite, ShouldMatchers, WordSpecLike}
//
///**
//  * Created by xinszhou on 11/9/16.
//  */
//class LeaderTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//  with ShouldMatchers with WordSpecLike {
//
//  def this() = this(ActorSystem("Leader-Test-Actor-System"))
//
//
////  val leader1 = _system.actorOf(Props(classOf[Leader],
////    List(acceptor1, acceptor2, acceptor3),
////    List(replica1, replica2, replica3)),
////    "leader-1")
//
//
//
//  "single leader must" must {
//    val tempLeader = _system.actorOf(Leader.props(Set(), Set()), "temp-leader")
//
//    "start with inactive status" in {
//      Leader.getLeaderStatus(tempLeader).active should be (false)
//    }
//
//    "store proposal to list when inactive" in {
//      tempLeader ! ProposalMessage(1, Command("clientId", "reqId", TestOperation))
//      val state = Leader.getLeaderStatus(tempLeader)
//      state.active should be (false)
//      state.proposals should contain (1-> ProposalMessage(1, Command("clientId", "reqId", TestOperation)))
//
//    }
//
//    "become active after receive adopted message" in {
//      tempLeader ! AdoptedMessage(BallotNumber("leader", 1), Set.empty[PValue])
//      val state = Leader.getLeaderStatus(tempLeader)
//      state.active should be (true)
//      state.proposals should contain (1-> ProposalMessage(1, Command("clientId", "reqId", TestOperation)))
//      state.proposals should have size 1
//    }
//
//  }
//
//
//  "leader work with acceptor and replica" must {
//
//    val acceptor1 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-1")
//    val acceptor2 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-2")
//    val acceptor3 = _system.actorOf(Props(classOf[Acceptor]), "acceptor-3")
//
//    val replica1 = _system.actorOf(ListStateReplica.props, "replica-1")
//    val replica2 = _system.actorOf(ListStateReplica.props, "replica-2")
//    val replica3 = _system.actorOf(ListStateReplica.props, "replica-3")
//
//    val tempLeader = _system.actorOf(Leader.props(Set(acceptor1, acceptor2, acceptor3),
//      Set(replica1, replica2, replica3)), "temp-leader-2")
//
//    val cmd = Command("clientId", "reqId", TestOperation)
//
//    "leader become active, acceptor update ballot number after initilization" in {
//
//      Thread.sleep(1000)
//
//      val leaderState = Leader.getLeaderStatus(tempLeader)
//      val acceptorState = Acceptor.getAcceptorStatus(acceptor1)
//
//      leaderState.active should be (true)
//      acceptorState.maxBallotNumSeenSoFar.round should be (1)
//    }
//
//    "acceptor got the new message that leader proposed" in {
//
//      val proposeMsg = ProposalMessage(1, cmd)
//      tempLeader ! proposeMsg
//      Thread.sleep(1000)
//
//      val leaderState = Leader.getLeaderStatus(tempLeader)
//      val acceptorState1 = Acceptor.getAcceptorStatus(acceptor1)
//
//      leaderState.proposals should contain (1 -> proposeMsg)
//      acceptorState1.acceptedNumber should contain (1 -> PValue(BallotNumber("leaderId", 1), 1, cmd))
//    }
//
//    "replicas has learned value" in {
//      val replicaState = Replica.getInternalState(replica1)
//      replicaState.slotOut should be (2)
//      replicaState.decision should contain (1 -> cmd)
//    }
//  }
//
//
//
//
//
//
//
//}
