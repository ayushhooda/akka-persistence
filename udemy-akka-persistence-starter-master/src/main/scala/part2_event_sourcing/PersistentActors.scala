package part2_event_sourcing

import java.util.Date

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors extends App {

  /*
  Scenario: we have a business and an accountant which keeps track of our invoices.
   */

  // COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)
  case class InvoiceBulk(invoices: List[Invoice])

  // Special messages
  case object Shutdown

  // EVENTS
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  class Accountant extends PersistentActor with ActorLogging {

    var latestInvoiceId = 0
    var totalAmount = 0

    override def persistenceId: String = "simple-accountant" // make it unique

    /**
      * The "normal" receive method
      */
    override def receiveCommand: Receive = {
      case Invoice(rec, date, amount) =>
        /*
          When you receive a command
          1. create an EVENT to persist into the store
          2. persist the EVENT, then pass in a callback that will get triggered once the event is written
          3. update the actor's state when the event has persisted
         */
        log.info(s"Receive invoice for amount: $amount")
        val event = InvoiceRecorded(latestInvoiceId, rec, date, amount)
        persist(event) /* time gap: all other msgs sent to this actor are stashed */{ e =>
          // SAFE to access mutable state here

          // update state
          latestInvoiceId += 1
          totalAmount += amount

//          sender() ! "PersistenceACK"

          log.info(s"Persisted $e as invoice ${e.id}, for total amount $totalAmount")
        }

      case InvoiceBulk(invoices) =>
        /*
          1) Create events (plural)
          2) persist all the events
          3) update the actor state when each event is persisted
         */
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map {
          case (invoice, id) => InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }
        persistAll(events) { e =>
          // update state
          latestInvoiceId += 1
          totalAmount += e.amount
          log.info(s"Persisted SINGLE $e as invoice ${e.id}, for total amount $totalAmount")
        }

      case Shutdown =>
        context.stop(self)
    }

    /**
      * Handler that will be called on recovery
      */
    override def receiveRecover: Receive = {
      /*
        best practice follow the logic in the persist steps of receiveCommand
       */
      case InvoiceRecorded(id, _, _, amount) =>
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Recovered invoice #$id for amount $amount, total amount: $totalAmount")
    }

    /*
      This method is called if persisting failed
      The actor will be STOPPED.

      Best practice: start the actor again after a while.
      (use Backoff supervisor)
     */
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persist $event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    /*
      Called if the Journal fails to persist the event
      The actor is RESUMED.

     */
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persist rejected for $event because of $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val system = ActorSystem("PersistentActors")

  val accountant = system.actorOf(Props[Accountant], "simpleAccountant")

  /*for (i <- 1 to 10) {
    accountant ! Invoice("The Sofa Company", new Date, i *1000)
  }*/

  /**
    * Persisting multiple events
    *
    * persistAll
    */
  val newInvoices = for (i <- 1 to 5) yield Invoice("The awesome chair", new Date, i * 2000)
  //  accountant ! InvoiceBulk(newInvoices.toList)

  /*
    NEVER EVER CALL PERSIST OR PERSISTALL FROM FUTURES
   */

  /**
    * Shutdown of Persistence actors
    *
    * best practice: define your own shutdown messages
    */
  accountant ! Shutdown


}

// Persistent Actors
/*
- can do everything a normal actor can do
- hold internal state
- run in parallel with many other actors

Extra capabilities
- have a persistence ID [Unique] -
- persists events to a long-term store
- recover state by replaying events from the store

When an actor handles a message(command)
- it can (asynchronously) persist an event to the store
- after the event is persisted, it changes its internal state

When an actor starts/restarts
- it replays all events with its persistence ID
 */


