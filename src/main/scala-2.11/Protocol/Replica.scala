package Protocol

import java.util

import Protocol.Messages._
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, FSM}
import java.util.{Map => JMap, Queue => JQueue}

import Protocol.Replica.{DoProposal, LookUpReplicaState, ReplicaState}

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.SortedMap
import scala.concurrent.Await

/**
  * Created by xinszhou on 11/9/16.
  */
object Replica {

  // controlled the seq
  val WINDOW = 500

  implicit val timeout = Timeout(3 second)

  case object DoProposal

  case object LookUpReplicaState

  case class ReplicaState(slotIn: Int, slotOut: Int,
                          clientInfo: Map[String, ActorRef],
                          requests: List[Command],
                          proposals: List[ProposalMessage],
                          decision: SortedMap[Int, Command],
                          rsm: Any)

  def getInternalState(replica: ActorRef): ReplicaState =
    Await.result(replica ? LookUpReplicaState, 3 second).asInstanceOf[ReplicaState]

}

/**
  * @todo missing, replay command after recovery from failure
  */
class Replica(numLeaders: Int) extends Actor with ActorLogging {

  this: ReplicateStateMachine =>

  val replicaId = "ReplicaId"

  var slot_in = 1
  var slot_out = 1

  // get leader actor selection
  val leaders: Set[ActorSelection] = (1 to numLeaders) map (i => context.actorSelection("../leader-" + i)) toSet

  //  var leaders: Set[ActorRef] = Set()
  var requests: List[Command] = List()
  var clientInfo: Map[String, ActorRef] = Map()

  var decisions: SortedMap[Int, Command] = SortedMap.empty[Int, Command]
  // better be a map
  var proposals: List[ProposalMessage] = List()

  import context.dispatcher
  context.system.scheduler.schedule(30 milli, 30 milli, self, DoProposal)


  def receive = {
    case DoProposal => propose

    /**
      * upon client request received
      * 1. check if proposals list already have the command, if so, do not send request twice
      * 2. if already too much pending requests, ignore this client request
      */
    case RequestMessage(cmd) if !proposals.exists(_ == cmd) =>
      log.info("request message received in replica actor")
      if (slot_out + Replica.WINDOW > slot_in) {
        clientInfo = clientInfo.updated(cmd.clientId, sender())
        requests = cmd :: requests
      } else {
        log.info("message is ignored")
      }

    /**
      * update decisions queue, but do not run RSM because decisions might not full
      * 1. if command is reconfiguration, do not execute command
      * 2. if command already been executed, skip command execution
      */
    case DecisionMessage(slotNumber, cmd) if slotNumber >= slot_out =>
      log.info("decision message received in replica actor")

      decisions = decisions.updated(slotNumber, cmd)

      while (decisions.isDefinedAt(slot_out)) {

        proposals.find(msg => msg.slotNumber == slot_out) match {
          case Some(msg) if msg.cmd == cmd => proposals = proposals.filterNot(_.slotNumber == slot_out)
          case Some(msg) => requests = msg.cmd :: requests
          case None =>

        }

        val toBeExecutedCmd = decisions.apply(slot_out)

        if (!toBeExecutedCmd.op.isInstanceOf[Config] && !decisions.exists { case (k, v) => k < slot_out && v == toBeExecutedCmd }) {
          log.info(s"replica execute $toBeExecutedCmd at index $slot_out")
          execute(toBeExecutedCmd)
          slot_out += 1
          clientInfo.get(toBeExecutedCmd.clientId).foreach(_ ! ResponseMessage(toBeExecutedCmd.clientId))

        } else {
          slot_out += 1
        }
      }


    case LookUpReplicaState =>
      this.internalState()
      sender() ! ReplicaState(slot_in, slot_out, clientInfo, requests, proposals, decisions, this.internalState())

    case msg =>
      log.warning(s"replica actor receive unknown message: $msg")

  }


  /**
    * slot_in should always > slot_out, so check those two value before sending proposal
    */
  def propose = {
    log.info("replica execute propose method")
    requests.foreach(cmd => {
      slot_in = Math.max(slot_in, slot_out)

      val proposal = ProposalMessage(slot_in, cmd)

      leaders.foreach(leader => leader ! proposal)

      proposals = proposal :: proposals

      slot_in += 1
    })
    requests = Nil

  }

}
