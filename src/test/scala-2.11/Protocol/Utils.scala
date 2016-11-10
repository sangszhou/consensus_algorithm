package Protocol

import Protocol.Messages.Command
import akka.actor.{ActorRef, Props}

/**
  * Created by xinszhou on 11/10/16.
  */
object Utils {

  object ListStateReplica {
    def props(numLeaders: Int) = Props(new ListStateReplica(numLeaders))
  }

  class ListStateReplica(numLeaders: Int) extends Replica(numLeaders) with ReplicateStateMachine {
    var state = List.empty[Command]

    override def execute(cmd: Command) = state = cmd :: state

    override def internalState: Any = state
  }

}
