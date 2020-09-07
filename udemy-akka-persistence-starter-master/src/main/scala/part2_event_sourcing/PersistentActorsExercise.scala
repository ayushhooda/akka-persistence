package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import scala.collection.mutable

object PersistentActorsExercise extends App {

  /*
    Persistent actor for a voting station
    Keep:
      - the citizens who voted
      - the poll: mapping between a candidate and the number of received votes so far

    The actor must be able to recover it's state if its shutdown or restarted
   */
  // Commands
  case class Vote(citizenPID: String, candidate: String)

  // Events
//  case class VoteRecorded(citizenPID: String, candidate: String)

  class VotingStation extends PersistentActor with ActorLogging {

    // state
    val citizens: mutable.Set[String] = new mutable.HashSet[String]()
    val poll: mutable.Map[String, Int] = new mutable.HashMap[String, Int]()

    override def persistenceId: String = "simple-voting-station"

    override def receiveCommand: Receive = {
      case vote @ Vote(citizenPID, candidate) =>
        /*
          1) create the event
          2) persist the event
          3) handle a state change after persisting is successful
         */
        // As Event structure is same as command, so we can directly persist command
        if (!citizens.contains(vote.citizenPID)) {
          persist(vote) { _ => // COMMAND Sourcing
            log.info(s"Persisted: $vote")
            handleInternalStateChange(vote)
          }
        } else {
          log.warning(s"Citizen $citizenPID is trying to vote multiple times")
        }
      case "print" =>
        log.info(s"Current State: $poll")
    }

    def handleInternalStateChange(vote: Vote): Unit = {
      citizens.add(vote.citizenPID)
      val votes = poll.getOrElse(vote.candidate, 0)
      poll.put(vote.candidate, votes + 1)
    }

    override def receiveRecover: Receive = {
      case vote @ Vote(citizenPID, candidate) =>
        log.info(s"Recovered: $vote")
        handleInternalStateChange(vote)
    }

  }

  val system = ActorSystem("PersistentActorExercise")
  val votingStation = system.actorOf(Props[VotingStation], "simpleVotingStation")

  val votesMap = Map[String, String](
    "Alice" -> "Martin",
    "Bob" -> "Roland",
    "Charlie" -> "Martin"
  )

//  votesMap.keys.foreach { citizen =>
//    votingStation ! Vote(citizen, votesMap(citizen))
//  }

  votingStation ! "print"
}
