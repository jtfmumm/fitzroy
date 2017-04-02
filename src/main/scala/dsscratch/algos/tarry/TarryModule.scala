package dsscratch.algos.tarry

import dsscratch.components._
import dsscratch.clocks.TimeStamp
import dsscratch.util.Rand
import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import dsscratch.algos._
import dsscratch.algos.nodes._

/*
Tarry's algorithm
Kicked off by one initiator
1) A process never forwards the token through the same channel twice
2) A process only forwards the token to its parent when there is no other
option
*/

trait TarryLocalState extends LocalState {
  var initiated: Boolean
  var initiator: Boolean
  var tokens: Queue[Token]
  var finishedTokens: Queue[Token]
  var parentChNodeId: ProcessId
  var nonParentChNodeIdsToSend: ArrayBuffer[ProcessId]
}

object TarryModule extends NodeModuleBuilder {
  def apply(parentNode: Node, nodeIds: Set[ProcessId],
    isInitiator: Boolean = false): TarryModule = {
    new TarryModule(parentNode, isInitiator)
  }
  def buildWith(parentNode: Node, s: TarryLocalState): TarryModule = {
    val newC = new TarryModule(parentNode, s.initiator)

    newC.s.initiated = s.initiated
    newC.s.tokens = s.tokens.map(x => x).toQueue
    newC.s.finishedTokens = s.finishedTokens.map(x => x).toQueue
    newC.s.parentChNodeId = s.parentChNodeId
    newC.s.nonParentChNodeIdsToSend = s.nonParentChNodeIdsToSend.map(x => x)
    newC
  }
}

class TarryModule(val parentNode: Node, isInitiator: Boolean = false) extends NodeModule {
  val algoCode: AlgoCode = AlgoCodes.TARRY

  ////////////////////
  //LOCAL STATE
  private object s extends TarryLocalState {
    var initiated = false
    var initiator: Boolean = isInitiator
    var tokens = Queue[Token]()
    var finishedTokens = Queue[Token]()
    var parentChNodeId: ProcessId = ProcessId.empty
    var nonParentChNodeIdsToSend = ArrayBuffer[ProcessId]()
  }
  ////////////////////

  def processMessage(cmd: Command, chSenderId: ProcessId, ts: TimeStamp): Unit = {
    cmd match {
      case ProcessToken(t) => processToken(t, chSenderId)
      case _ =>
    }
  }

  def terminated = s.finishedTokens.nonEmpty

  def step(): Unit = {
    if (s.initiator && !s.initiated) initiate()
    if (s.tokens.isEmpty && s.finishedTokens.isEmpty) return
    s.nonParentChNodeIdsToSend.size match {
      case 0 if hasNoParent && s.tokens.nonEmpty => {
        val t = s.tokens.dequeue()
        s.finishedTokens.enqueue(t)
      }
      case 0 if hasNoParent =>
      case 0 => {
        sendToken(s.parentChNodeId)
        val t = s.tokens.dequeue() //Parent is the last destination for token
        s.finishedTokens.enqueue(t)
        emptyParent()
      }
      case _ => {
        val randNodeIdIndex =
          Rand.rollFromZero(s.nonParentChNodeIdsToSend.size)
        val nodeId = s.nonParentChNodeIdsToSend.remove(randNodeIdIndex)
        sendToken(nodeId)
      }
    }
  }

  def snapshot: TarryModule = TarryModule.buildWith(parentNode, s)

  def result = {
    if (s.parentChNodeId != ProcessId.empty)
      "  Node" + s.parentChNodeId + " -> " + parentNode + ";\n"
    else
      ""
  }

  private def initiate(): Unit = {
    val t = Token(parentNode.id)
    s.tokens.enqueue(t)
    log.write("[Tarry] Initiator: No Parent", clock.stamp())
    s.nonParentChNodeIdsToSend =
      ArrayBuffer(outProcessIds.filter(_ != s.parentChNodeId): _*)
    val firstNodeId = Rand.pickItem(outProcessIds)
    val cmd = ProcessToken(t)
    val msg = Message(cmd, parentNode.id, clock.stamp())
    log.write("[Tarry] Sending to Node" + firstNodeId, msg.ts)
    parentNode.send(firstNodeId, msg)
    val firstChNodeIdIndex = s.nonParentChNodeIdsToSend.indexOf(firstNodeId)
    s.nonParentChNodeIdsToSend.remove(firstChNodeIdIndex)
    s.initiated = true
  }

  private def hasNoParent: Boolean =
    s.parentChNodeId == ProcessId.empty

  private def sendToken(chNodeId: ProcessId) = {
    val pt = ProcessToken(s.tokens.last)
    val msg = Message(pt, parentNode.id, clock.stamp())
    log.write("[Tarry] Sending to Node" + chNodeId, msg.ts)
    parentNode.send(chNodeId, msg)
  }

  private def processToken(t: Token, senderId: ProcessId): Unit = {
    if (s.tokens.isEmpty && s.parentChNodeId == ProcessId.empty &&
      s.finishedTokens.isEmpty)
    {
      s.parentChNodeId = senderId
      log.write("[Tarry] Parent: Node" + s.parentChNodeId, clock.stamp())
      s.parentChNodeId = outProcessIds.filter(_ == senderId).head
      s.nonParentChNodeIdsToSend =
        ArrayBuffer(outProcessIds.filter(_ != s.parentChNodeId): _*)
      s.tokens.enqueue(t)
    }
  }

  private def emptyParent(): Unit = {
    s.parentChNodeId = ProcessId.empty
  }
}
