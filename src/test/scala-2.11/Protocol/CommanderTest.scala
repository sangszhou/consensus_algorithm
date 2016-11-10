package Protocol

import Protocol.Messages.{BallotNumber, Command, TestOperation}
import akka.actor.{ActorRef, ActorSystem}
import org.scalatest.FunSuite

/**
  * Created by xinszhou on 11/10/16.
  */
class CommanderTest extends FunSuite {

  val system = ActorSystem("commander-center")

  test("creating commander actor") {
    val actor = system.actorOf(Commander.props(Set.empty[ActorRef],
      Set.empty[ActorRef], 1, BallotNumber("leader", 1), Command("client", "reqId", TestOperation)),
      name = "commander")

    println("executed commander")

    1 === 1

  }
}
