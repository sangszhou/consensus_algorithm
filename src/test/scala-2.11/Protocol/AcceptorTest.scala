//package Protocol
//
//import Protocol.Acceptor.{AcceptorInternalState, LookUpAcceptor}
//import Protocol.Messages._
//import akka.actor.{ActorSystem, Props}
//import akka.testkit.{ImplicitSender, TestKit}
//import org.scalatest.{BeforeAndAfterAll, FunSuite, ShouldMatchers, WordSpecLike}
//import akka.pattern.ask
//import akka.util.Timeout
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//
///**
//  * Created by xinszhou on 11/9/16.
//  */
//class AcceptorTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//  with ShouldMatchers with WordSpecLike {
//
//  implicit val timeout = Timeout(10 seconds)
//
//  def this() = this(ActorSystem("AcceptorTestActorSystem"))
//
//  val acceptor = _system.actorOf(Props(classOf[Acceptor]), "accetpor-1")
//
//  "an acceptor" must {
//
//    val ballotNumber1 = BallotNumber("leader1", 1)
//    val phase1a = Phase1a(ballotNumber1)
//
//    "accept the first ballot number" in {
//      acceptor ! phase1a
//
//      expectMsg(Phase1b(ballotNumber1, List.empty[PValue]))
//    }
//
//    "reject older ballot number" in {
//      val phase1a_old = BallotNumber("leader1", 0)
//      acceptor ! phase1a_old
//      expectNoMsg(3 second)
//    }
//
//    case object SampleOperation extends Operation
//    val cmd = Command("clientId", "1", SampleOperation)
//    val phase2a = Phase2a("src", ballotNumber1, 1, cmd)
//
//    "accept phase2a with same ballot number" in {
//      acceptor ! phase2a
//      expectMsg(3 second, Phase2b("src", ballotNumber1, 1))
//
//      val acceptorState = getAcceptorInternal
//      acceptorState.acceptedNumber should be (List(PValue(ballotNumber1, 1, cmd)))
//    }
//
//  }
//
//  def getAcceptorInternal = Await.result(acceptor ? LookUpAcceptor, 10 second).asInstanceOf[AcceptorInternalState]
//
//}
