package Protocol

import Protocol.Messages.BallotNumber
import org.scalatest.FunSuite

/**
  * Created by xinszhou on 11/10/16.
  */
class Messages$Test extends FunSuite {

  test("ballot number compare") {
    var ballot1 = BallotNumber("leader1", 1)
    val ballot2 = BallotNumber("leader2", 1)

    assert(ballot1 < ballot2)

    ballot1 = BallotNumber("leader3", 1)

    assert(ballot1 > ballot2)
  }

}
