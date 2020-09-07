package part2_event_sourcing

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

object MultiplePersists extends App {

  /*
    Diligent accountant: with every invoice, persist TWO events
    - a tax record for the fiscal authority
    - an invoice record for personal logs or some auditing authority
   */

  // COMMANDS
  case class Invoice(recipient: String, date: Date, amount: Int)

  // EVENTS
  case class TaxRecord(taxId: String, recordId: Int, date: Date, totalAmount: Int)
  case class InvoiceRecord(invoiceRecordId: Int, recipient: String, date: Date, amount: Int)

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef) = Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {

    var latestTaxRecordId = 0
    var latestInvoiceRecordId = 0

    override def persistenceId: String = "diligent-accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        // Internally it's something like : journal ! TaxRecord
        persist(TaxRecord(taxId, latestTaxRecordId, date, amount / 3)) { tr =>
          taxAuthority ! tr
          latestTaxRecordId += 1

          // nested persist
          persist("I hereby declare this tax record to be true and complete.") { declaration =>
            taxAuthority ! declaration
          }
        }
        // journal ! InvoiceRecord
        persist(InvoiceRecord(latestInvoiceRecordId, recipient, date, amount)) { ir =>
          taxAuthority ! ir
          latestInvoiceRecordId += 1

          // nested persist
          persist("I hereby declare this invoice record to be true") { declaration =>
            taxAuthority ! declaration
          }
        }

    }

    override def receiveRecover: Receive = {
      case event => log.info(s"Recovered: $event")
    }
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(s"Received: $message")
    }
  }

  val system = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant = system.actorOf(DiligentAccountant.props("UK2555_55544", taxAuthority))

  accountant ! Invoice("The Sofa Company", new Date, 2000)

  /*
    The message ordering (TaxRecord -> InvoiceRecord) is GUARANTEED
   */

  // Persistence is based on messages

}
