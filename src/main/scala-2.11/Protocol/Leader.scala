package Protocol

import Protocol.Messages._
import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by xinszhou on 11/9/16.
  */

object Leader {

  implicit val timeout = Timeout(3 second)

  def pmax(pvals: Set[PValue]): Map[Int, PValue] = {
    pvals.groupBy(_.slot_number)
      .map { case (slotNum, pvalSet) => (slotNum, pvalSet.maxBy(_.ballotNumber)) }
  }

  /**
    * x ▹ y returns everything in y and (x-y)
    */
  def ▹(proposals: Map[Int, ProposalMessage], map: Map[Int, PValue]): Map[Int, ProposalMessage] = {
    var combined = proposals
    map.foreach { case (slot, pv) => combined = combined.updated(slot,
      ProposalMessage(slot, pv.cmd))
    }
    combined
  }

  def props(acceptors: Set[ActorRef], replicas: Set[ActorRef]) = Props(new Leader(acceptors, replicas))


  // for testing
  case object LookUpStatus

  case class LeaderInternalState(proposals: Map[Int, ProposalMessage],
                                 currentBallotNumber: BallotNumber,
                                 active: Boolean)

  def getLeaderStatus(leader: ActorRef): LeaderInternalState =
    Await.result(leader ? LookUpStatus, 3 second).asInstanceOf[LeaderInternalState]

  @volatile
  var index = 1

  def getIndex: Int = {
    val temp = index
    index += 1
    temp
  }

}

class Leader(var acceptors: Set[ActorRef], var replicas: Set[ActorRef]) extends Actor with ActorLogging {

  import Leader._

  var proposals: Map[Int, ProposalMessage] = Map.empty[Int, ProposalMessage]
  var currentBallotNumber = BallotNumber(self.path.name, 1)

  // for test
  @volatile
  var isActive = false

  def setActive(result: Boolean): Unit = {
    log.debug(s"set isActive to $result")
    isActive = result
  }

  override def preStart() = {
    context.actorOf(Scout.props(acceptors, currentBallotNumber),
      "scout-actor-for-leader" + Random.nextLong())
  }

  override def receive: Receive = inActive orElse shared

  def inActive: Receive = {

    // 能不能对 cmd 进行重复过滤?
    // 假如过滤的话, slotNumber 就会被浪费了, 导致 rsm 中间有了个缝
    // 但不确定是否完全正确
    case msg@ProposalMessage(slotNumber, cmd) =>

      log.info(s"leader receive proposal message from replica $msg in inactive mode")
      proposals.get(slotNumber) match {
        case None => proposals = proposals.updated(slotNumber, msg)
        case _ =>
      }

    case AdoptedMessage(ballotNumber, accepted) =>

      setActive(true)
      log.info(s"win leader election with ballotNum $ballotNumber, accepted $accepted")

      val slotValue = pmax(accepted)
      proposals = ▹(proposals, slotValue)
      // release commander actor
      proposals.foreach { case (slotNum: Int, proposal: ProposalMessage) =>
        context.actorOf(Commander.props(acceptors, replicas, slotNum, currentBallotNumber, proposal.cmd),
          name = "commander-actor-" + Random.nextLong())
      }

      context.become(active orElse shared)
  }

  def active: Receive = {

    case msg@ProposalMessage(slotNumber, cmd) =>
      log.info(s"leader receive proposal message in active mode slot $slotNumber, cmd: $cmd")
      proposals.get(slotNumber) match {
        case None =>
          proposals = proposals.updated(slotNumber, msg)
          // release commander actor
          context.actorOf(Commander.props(acceptors, replicas, slotNumber, currentBallotNumber, cmd),
            "commander-actor-" + Random.nextLong())
        case _ =>
      }
  }

  def shared: Receive = {
    case PreemptedMessage(ballotNumber: BallotNumber) =>
      log.info("preemptedMessage received in leader")
      if (ballotNumber > currentBallotNumber)
        currentBallotNumber = currentBallotNumber.copy(round = ballotNumber.round + 1)

      // another round of leader election
      // @todo backoff
      setActive(false)
      context.actorOf(Scout.props(acceptors, currentBallotNumber), "scout-actor-for-leader" + +Random.nextLong())
      context.become(inActive orElse shared)

    case LookUpStatus =>
      log.info("look up acceptor status")
      sender() ! LeaderInternalState(proposals, currentBallotNumber, isActive)

    case _ =>
  }


}
