package Protocol

/**
  * Created by xinszhou on 11/9/16.
  */
object Messages {

  trait Operation

  case object TestOperation extends Operation

  case object op1 extends Operation
  case object op2 extends Operation
  case object op3 extends Operation
  case object op4 extends Operation

  case class Config(replicas: List[String], acceptors: List[String], leaders: List[String]) extends Operation

  case class Command(clientId: String, reqId: String, op: Operation)

  case class BallotNumber(leaderId: String, round: Int) extends Ordered[BallotNumber] {
    override def compare(that: BallotNumber): Int = this.round - that.round match {
      case 0 => this.leaderId compare that.leaderId
      case nonZero => nonZero
    }
  }

  case class PValue(ballotNumber: BallotNumber, slot_number: Int, cmd: Command)

  trait Message {
  }

  case class Phase1a(ballotNumber: BallotNumber) extends Message

  case class Phase1b(ballotNumber: BallotNumber, accepted: Map[Int, PValue]) extends Message

  case class Phase2a(ballotNumber: BallotNumber, slotNumber: Int, cmd: Command) extends Message

  // no nack
  case class Phase2b(ballotNumber: BallotNumber, slotNumber: Int) extends Message

  // from scout to leader
  case class PreemptedMessage(ballotNumber: BallotNumber) extends Message

  // from scout to leader
  case class AdoptedMessage(ballotNumber: BallotNumber, accepted: Set[PValue]) extends Message

  // from commander thread to replica and leader
  case class DecisionMessage(slotNumber: Int, cmd: Command) extends Message

  // from client
  case class RequestMessage(cmd: Command) extends Message

  // response message to client
  case class ResponseMessage(src: String) extends Message

  // proposed by replica
  case class ProposalMessage(slotNumber: Int, cmd: Command) extends Message

}
