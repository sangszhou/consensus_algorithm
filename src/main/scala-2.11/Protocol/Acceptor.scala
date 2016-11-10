package Protocol

import Protocol.Acceptor.{AcceptorInternalState, LookUpAcceptor}
import Protocol.Messages._
import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout

/**
  * Created by xinszhou on 11/9/16.
  */

object Acceptor {
  implicit val timeout = Timeout(3 second)

  case object LookUpAcceptor
  case class AcceptorInternalState(maxBallotNumSeenSoFar: BallotNumber,
                                   acceptedNumber: Map[Int, PValue])

  def getAcceptorStatus(acceptor: ActorRef): AcceptorInternalState =
    Await.result(acceptor ? LookUpAcceptor, 3 second).asInstanceOf[AcceptorInternalState]
}

class Acceptor extends Actor with ActorLogging {

  var maxBallotNumSeenSoFar: BallotNumber = BallotNumber("", -1)

  /**
    *  key is slot, value is PValue which include ballotNumber as member
    */
  var acceptedValues = Map.empty[Int, PValue]


  override def receive: Receive = {
    /**
      * @todo what if ballotNum equals maxBallotNumber? should reply too, because resend phase1a
      */
    case Phase1a(ballotNumber) =>

      if (ballotNumber > maxBallotNumSeenSoFar) {
        log.info(s"update ballotNumber to $ballotNumber")
        maxBallotNumSeenSoFar = ballotNumber
      }

      sender() ! Phase1b(maxBallotNumSeenSoFar, acceptedValues)

    /**
      * why didn't respond when ballotNumber bigger than itself ?
      * 为什么 phase2 不会更新 ballotNumber 呢?
      */
    case Phase2a(ballotNumber, slotNumber, cmd) =>
      if(ballotNumber == maxBallotNumSeenSoFar) {
        // 这个不确定, 如果两个 leader 发送了相同 slot number 但是值不一样, 这里改不改更新 acceptedValue map 呢
        // 修改肯定是对 acceptor 来说是不对的, 但是即使这边发生了修改, 也不会影响 replica 的值
        acceptedValues = acceptedValues.updated(slotNumber, PValue(ballotNumber, slotNumber, cmd))
      }

      log.info("send phase2b to commander")

      // why send phase2b what ever
      sender() ! Phase2b(maxBallotNumSeenSoFar, slotNumber)

    case LookUpAcceptor =>
      sender() ! AcceptorInternalState(maxBallotNumSeenSoFar, acceptedValues)

    case _ =>
      log.debug("unknown message received in acceptor")
  }

}

/**
  * how to make late acceptor catch up with new data?
  * 1. new leader with new view change protocol, can let acceptor know new value
  */
