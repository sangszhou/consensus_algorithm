package Protocol

import Protocol.Messages._
import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

/**
  * Created by xinszhou on 11/9/16.
  */

object Commander {
  def props(acceptors: Set[ActorRef],
            replicas: Set[ActorRef],
            slotNumber: Int,
            ballotNumber: BallotNumber,
            cmd: Command) = Props(new Commander(acceptors, replicas, slotNumber, ballotNumber, cmd))
}

class Commander(acceptors: Set[ActorRef],
                replicas: Set[ActorRef],
                slotNumber: Int,
                ballotNumber: BallotNumber,
                cmd: Command) extends Actor with ActorLogging {

  var waitForResponse = acceptors

  override def preStart(): Unit = acceptors.foreach(_ ! Phase2a(ballotNumber, slotNumber, cmd))

  override def receive: Receive = {
    case Phase2b(_ballotNumber: BallotNumber, slotNumber: Int) =>
      val acceptor = sender()

      if (_ballotNumber > ballotNumber) {
        log.info("commander detected bigger ballotNumber, send preempted message to leader and" +
          "start another leader election")
        context.parent ! PreemptedMessage(_ballotNumber)
        context.stop(self)
      } else if (_ballotNumber == ballotNumber && waitForResponse.contains(acceptor)) {
        waitForResponse -= acceptor
        if (waitForResponse.size < (replicas.size + 1) / 2) {
          replicas.foreach(_ ! DecisionMessage( slotNumber, cmd))
//          context.parent ! DecisionMessage(slotNumber, cmd)
          context.stop(self)
        }
      }
  }
}
