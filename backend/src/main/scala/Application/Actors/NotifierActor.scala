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

case class GameStateTransfer(gameState:String,data:GameState)

object NotifierActor {

  def apply(connections : HashMap[String,ActorRef[String]]) : Behavior[NotifyCommand] = Behaviors.receive { (ctx, msg) =>
    msg match
      case AddConnection(id,replyTo) =>
        val newConnections = connections.updated(id,replyTo)
        ctx.log.info("New connections :"+newConnections.toString())
        this(newConnections)
      case RemoveConnection(id) => this(connections.removed(id))

      case NotifyWith(state) =>
        state match
        case WaitingState(players) =>
          val connectedPlayers = players.flatMap(l => if(connections.contains(l)) then Some(l) else None)
          val dataToSend = AddTypeToJson(WaitingState(players).asJson,"waitingState").noSpaces
          connectedPlayers.foreach(l => connections(l) ! dataToSend)
          ctx.log.info("Sent :" + dataToSend)
          Behaviors.same
        case PlayingState(rnd,trn,players, apok,bnk) =>
          val connectedPlayers = players.flatMap { (l, d) => if (connections.contains(l)) then
            Some(l) else None
          }
          connectedPlayers.foreach(l =>
            {
              val obfuscated = ObfuscateDeckForPlayer(l, PlayingState(rnd, trn, players, apok, bnk))
              val dataToSend = AddTypeToJson(obfuscated.asJson,"playingState").noSpaces
              ctx.log.warn(dataToSend.toString())
              connections(l) ! dataToSend
            })
          ctx.log.info("Sent :" + PlayingState(rnd,trn,players, apok,bnk).asJson.noSpaces)
          Behaviors.same
        case GameEnded(players,apok,bnk,thr) =>
          val connectedPlayers = players.flatMap((l, d) => if (connections.contains(l)) then Some(l) else None)
          val dataToSend = AddTypeToJson(GameEnded(players,apok,bnk,thr).asJson,"gameEnded").noSpaces
          connectedPlayers.foreach(l => connections(l) ! dataToSend)
          ctx.log.info("Sent :" + dataToSend)
          Behaviors.same
        case VotingState(playersAndVotes,rnd,apok,bnk) =>
          val connectedPlayers = playersAndVotes.flatMap((l, o,d) => if (connections.contains(l)) then Some(l) else None)
          val dataToSend = AddTypeToJson(VotingState(playersAndVotes,rnd,apok,bnk).asJson,"votingState").noSpaces
          connectedPlayers.foreach(l => connections(l) ! dataToSend)
          ctx.log.info("Sent :"+ dataToSend)
          Behaviors.same


  }
}

def ObfuscateDeckForPlayer(uId:String,state:GameState) : Option[PlayingState] = {
  def DeleteCardIfNotRevealed(cards : Seq[(Card,Boolean)]) : Seq[(Card,Boolean)] = {
    cards.filter((c,b) => b).map((c,b) => (c,true))
  }
  state match
    case PlayingState(rnd,trn,players,apok,bnk)=>
      val newPlayers = players.map((id,cards) => if(id == uId) then  (id,cards) else (id,DeleteCardIfNotRevealed(cards)))
      val newBnk = DeleteCardIfNotRevealed(bnk)
      Some(PlayingState(rnd,trn,newPlayers,apok,newBnk))
    case _ => None
}

def AddTypeToJson(json:Json,typeMark:String) : Json = {
  val value = typeMark.asJson
  val newField = Json.obj("type" -> value)
  json.deepMerge(newField)
}