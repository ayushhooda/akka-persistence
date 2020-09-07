package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable

object Snapshots extends App {

  /**
    * Problem: Long-lived entities take a long time to recover
    * Solution: save checkpoints
    */

  // commands
  case class ReceivedMessage(contents: String) // message from your contact
  case class SentMessage(contents: String) // message TO your contact

  // events
  case class ReceivedMessageRecord(id: Int, contents: String)
  case class SentMessageRecord(id: Int, contents: String)


  object Chat {
    def props(owner: String, contact: String) = Props(new Chat(owner, contact))
  }

  class Chat(owner: String, contact: String) extends PersistentActor with ActorLogging {

    val MAX_MESSAGES = 10
    var currentMessageId = 0
    val lastMessages: mutable.Queue[(String, String)] = new mutable.Queue[(String, String)]()
    var commandsWithoutCheckpoint = 0

    override def persistenceId: String = s"$owner-$contact-chat"

    override def receiveCommand: Receive = {
      case ReceivedMessage(contents) =>
        persist(ReceivedMessageRecord(currentMessageId, contents)) { _ =>
          log.info(s"Received message: $contents")
          mayBeReplaceMessage(contact, contents)
          currentMessageId += 1
          mayBeCheckpoint()
        }
      case SentMessage(contents) =>
        persist(SentMessageRecord(currentMessageId, contents)) { _ =>
          log.info(s"Sent message: $contents")
          mayBeReplaceMessage(owner, contents)
          currentMessageId += 1
          mayBeCheckpoint()
        }
      case "print" =>
        log.info(s"Messages: $lastMessages")
        // snapshot related messages
      case SaveSnapshotSuccess(metadata) =>
        log.info(s"Saving snapshot succeeded: $metadata")
      case SaveSnapshotFailure(metadata, cause) =>
        log.warning(s"Saving snapshot failed: $metadata with cause: $cause")
    }

    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id, contents) =>
        log.info(s"Recovered received message $id: $contents")
        mayBeReplaceMessage(contact, contents)
        currentMessageId = id
      case SentMessageRecord(id, contents) =>
        log.info(s"Recovered sent message $id: $contents")
        mayBeReplaceMessage(owner, contents)
        currentMessageId = id
      case SnapshotOffer(metadata, snapshot) =>
        log.info(s"Recovered Snapshot: $metadata")
        snapshot.asInstanceOf[mutable.Queue[(String, String)]].foreach(lastMessages.enqueue(_))
    }

    def mayBeReplaceMessage(sender: String, contents: String): Unit = {
      if (lastMessages.size >= MAX_MESSAGES) {
        lastMessages.dequeue()
      }
      lastMessages.enqueue((sender, contents))
    }

    def mayBeCheckpoint(): Unit = {
      commandsWithoutCheckpoint += 1
      if (commandsWithoutCheckpoint >= MAX_MESSAGES) {
        log.info("Saving checkpoint...")
        saveSnapshot(lastMessages)
        commandsWithoutCheckpoint = 0
      }
    }

  }

  val system = ActorSystem("SnapshotsDemo")
  val chat = system.actorOf(Chat.props("daniel123", "martin345"))

//  for (i <- 1 to 1000) {
//    chat ! ReceivedMessage(s"Akka rocks $i")
//    chat ! SentMessage(s"Akka rules $i")
//  }

  chat ! "print"

  /**
    * Now this will take some time to recover, so snapshots comes to the rescue
    * Snapshots saving are asynchronous
    * Snapshots = saving entire state as checkpoints (saveSnapshot(state))
    * pattern:
    * 1. after each persist, maybe save a snapshot (logic is up to you)
    * 2. if you save a snapshot, handle the SnapshotOffer message in receiveRecover
    * 3. (optional, but best practice) handle SaveSnapshotSuccess and SaveSnapshotFailure messages in receiveCommand
    */

  /**
    * while recovery, only last snapshot is receovered and events after that snapshot.
    * That is why, it is so fast.
    */

}
