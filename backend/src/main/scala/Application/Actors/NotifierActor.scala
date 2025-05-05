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
case class AddConnection(id:String,replyTo:ActorRef[String]) extends NotifyCommand
case class RemoveConnection(id:String) extends NotifyCommand
case class NotifyWith(state: GameState) extends NotifyCommand

object NotifierActor {

  def apply(connections : HashMap[String,ActorRef[String]]) : Behavior[NotifyCommand] = Behaviors.receive { (ctx, msg) =>
    msg match
      case AddConnection(id,replyTo) => this(connections.updated(id,replyTo))
      case RemoveConnection(id) => this(connections.removed(id))

      case NotifyWith(state) =>
        state match
        case WaitingState(players) =>
          ctx.log.info("Connected :",players.toString())
          val connectedPlayers = players.flatMap(l => if(connections.contains(l)) then Some(l) else None)
          connectedPlayers.foreach(l => connections(l) ! WaitingState(players).asJson.noSpaces)
          ctx.log.info("Sent :",WaitingState(players).asJson.noSpaces)
          Behaviors.same
        case PlayingState(rnd,trn,players, apok,bnk) =>
          val connectedPlayers = players.flatMap { (l, d) => if (connections.contains(l)) then
            Some(l) else None
          }
          connectedPlayers.foreach(l => connections(l) ! ObfuscateDeckForPlayer(
            l,PlayingState(rnd,trn,players,apok,bnk)).asJson.noSpaces)

          ctx.log.info("Sent :",PlayingState(rnd,trn,players,apok,bnk).asJson.noSpaces)
          Behaviors.same
        case GameEnded(players,apok,bnk,thr) =>
          val connectedPlayers = players.flatMap((l, d) => if (connections.contains(l)) then Some(l) else None)
          connectedPlayers.foreach(l => connections(l) ! GameEnded(players,apok,bnk,thr).asJson.noSpaces)
          ctx.log.info("Sent :",GameEnded(players,apok,bnk,thr).asJson.noSpaces)
          Behaviors.same
        case VotingState(playersAndVotes,rnd,apok,bnk) =>
          val connectedPlayers = playersAndVotes.flatMap((l, o,d) => if (connections.contains(l)) then Some(l) else None)
          connectedPlayers.foreach(l => connections(l) ! VotingState(playersAndVotes,rnd,apok,bnk).asJson.noSpaces)
          ctx.log.info("Sent :",VotingState(playersAndVotes,rnd,apok,bnk).asJson.noSpaces)
          Behaviors.same


  }
}

def ObfuscateDeckForPlayer(id:String,state:GameState) : Option[PlayingState] = {
  def DeleteCardIfNotRevealed(cards : Seq[(Card,Boolean)]) : Seq[(Card,Boolean)] = {
    cards.filter((c,b) => b).map((c,b) => (c,true))
  }
  state match
    case PlayingState(rnd,trn,players,apok,bnk)=>
      val newPlayers = players.map((id,cards) => (id,DeleteCardIfNotRevealed(cards)))
      Some(PlayingState(rnd,trn,newPlayers,apok,bnk))
    case _ => None
}
