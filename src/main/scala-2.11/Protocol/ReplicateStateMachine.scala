package Protocol

import Protocol.Messages.Command

/**
  * Created by xinszhou on 11/9/16.
  */
/**
  * @todo execute snapshot
  */

object ReplicateStateMachine {

}

trait ReplicateStateMachine {

  def execute(cmd: Command)

  // for testing
  def internalState(): Any
}
