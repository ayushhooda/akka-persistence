package part2_event_sourcing

import akka.actor.ActorLogging
import akka.persistence.PersistentActor

object PersistAsyncDemo extends App {

  /*
    High throughput use cases
    Event ordering is guaranteed
   */

  case class Command(contents: String)
  case class Event(contents: String)

  class CriticalStreamProcessor extends PersistentActor with ActorLogging {

    override def persistenceId: String = "critical-stream-processing"

    override def receiveCommand: Receive = ???

    override def receiveRecover: Receive = {
      case msg => log.info(s"Recovered: $msg")
    }
  }

}
