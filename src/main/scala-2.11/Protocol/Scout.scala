package Protocol

import Protocol.Messages._
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}

/**
  * Created by xinszhou on 11/9/16.
  */

object Scout {
  def props(acceptors: Set[ActorRef], ballotNumber: BallotNumber) = Props(new Scout(acceptors, ballotNumber))
}

class Scout(acceptors: Set[ActorRef], ballotNumber: BallotNumber) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    acceptors.foreach(_ ! Phase1a(ballotNumber))
  }

  var waitingAcceptor = acceptors
  var acceptedValues = Set.empty[PValue]

  var maxBallotNumberSeenSoFar = BallotNumber("", -1)

  /**
    * @todo
    * @return
    */

  override def receive: Receive = {
    case Phase1b(_ballotNumber, accepted) =>

      if (_ballotNumber > ballotNumber) {
        log.info("scout detect bigger ballotNumber, start another leader election")
        context.parent ! PreemptedMessage(ballotNumber)
        context.stop(self)
      } else {
        val acceptor = sender()

        // recognized by client
        if (_ballotNumber == ballotNumber && waitingAcceptor.contains(acceptor)) {
          waitingAcceptor = waitingAcceptor - acceptor
          acceptedValues ++= accepted.values.toSet
        }

        if (waitingAcceptor.size < (acceptors.size + 1) / 2) {
          log.info("collected enough message, send adopted message to leader")
          context.parent ! AdoptedMessage(ballotNumber, acceptedValues)
          context.stop(self)
        }
      }
  }
}
