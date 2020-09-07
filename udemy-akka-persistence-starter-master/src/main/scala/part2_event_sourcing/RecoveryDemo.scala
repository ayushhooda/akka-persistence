package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, Recovery, RecoveryCompleted}

object RecoveryDemo extends App {

  case class Command(content: String)
  case class Event(content: String)

  class RecoveryActor extends PersistentActor with ActorLogging {

    override def persistenceId: String = "recovery-actor"

    override def receiveCommand: Receive = {
      case Command(content) =>
        persist(Event(content)) { e =>
          log.info(s"Successfully persisted $e, recovery is ${if (this.recoveryFinished) "" else "NOT"} finished.")
        }
    }

    override def receiveRecover: Receive = {
      case RecoveryCompleted =>
        // can be used for additional initialization after recovery
        log.info(s"I have finished recovering")
      case Event(content) =>
        log.info(s"Recovered: $content")
    }

    override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.error(s"Recovery failed... for event $event")
      super.onRecoveryFailure(cause, event)
    }

    // can be used for debugging or for some specific use cases
    override def recovery: Recovery = {
      Recovery(toSequenceNr = 100) // will fetch only 100 events
//      super.recovery
    }

    // There are multiple ways you can override recovery()



  }

  val system = ActorSystem("RecoveryDemo")
  val recoveryActor = system.actorOf(Props[RecoveryActor], "recoveryActor")

  /*
    Stashing Commands
   */
  for (i <- 1 to 1000) {
    recoveryActor ! Command(s"Hello $i")
  }

  /**
    * 1. ALL COMMANDS sent during recovery are stashed
    * 2. Failure during recovery:
    *   a. onRecoveryFailure is called + actor is stopped
    * 3. Customizing recovery: override recovery() [DONOT persist more msgs after a customized recovery method as this will corrupt actor state]
    * 4. Recovery status or KNOWING when you're done recovering
    * 5. Getting a signal when u r done recovering
    * 6. Stateless actors - use context.become to achieve it
    */


}
