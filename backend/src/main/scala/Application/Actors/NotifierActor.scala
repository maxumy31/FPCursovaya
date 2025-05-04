package Application.Actors

import akka.actor.typed.{ActorRef, Behavior}
import Domain.*
import akka.actor.typed.scaladsl.Behaviors
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.Encoder
import scala.collection.immutable.HashMap


sealed trait NotifyCommand
case class AddConnection(id:Long,replyTo:ActorRef[String]) extends NotifyCommand
case class RemoveConnection(id:Long) extends NotifyCommand
case class NotifyWith(state: GameState) extends NotifyCommand

object NotifierActor {

  def apply(connections : HashMap[Long,ActorRef[String]]) : Behavior[NotifyCommand] = Behaviors.receive { (ctx, msg) =>
    msg match
      case AddConnection(id,replyTo) => this(connections.updated(id,replyTo))
      case RemoveConnection(id) => this(connections.removed(id))

      case NotifyWith(state) => state match
        case WaitingState(players) =>
          val connectedPlayers = players.flatMap(l => if(connections.contains(l)) then Some(l) else None)
          connectedPlayers.foreach(l => connections(l) ! WaitingState(players).asJson.noSpaces)
          Behaviors.same
        case PlayingState(rnd,trn,players) =>
          val connectedPlayers = players.flatMap((l,d) => if (connections.contains(l)) then Some(l) else None)
          connectedPlayers.foreach(l => connections(l) ! PlayingState(rnd,trn,players).asJson.spaces2)
          Behaviors.same
        case GameEnded(players) =>
          val connectedPlayers = players.flatMap((l, d) => if (connections.contains(l)) then Some(l) else None)
          connectedPlayers.foreach(l => connections(l) ! GameEnded(players).asJson.noSpaces)
          Behaviors.same
        case VotingState(playersAndVotes,rnd) =>
          val connectedPlayers = playersAndVotes.flatMap((l, o,d) => if (connections.contains(l)) then Some(l) else None)
          connectedPlayers.foreach(l => connections(l) ! VotingState(playersAndVotes,rnd).asJson.noSpaces)
          Behaviors.same


  }
}

