package Application.Actors

import Application.*
import Domain.*
import Transport.*
import akka.actor.Actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.TextMessage
import akka.util.Timeout

import scala.collection.immutable.{HashMap, HashSet}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}
import io.circe.*
import io.circe.parser.*
import io.circe.generic.auto._
import io.circe.syntax._

import Application.Actors.*

final case class WebsocketCommandWithReply(replyTo:ActorRef[String],command:WebsocketCommand)
sealed trait WebsocketCommand
final case class ReserveNewConnection(guestId:String, sessionId : String) extends WebsocketCommand
final case class StartGameWSCommand(sessionId:String,initiator:String) extends WebsocketCommand
final case class MakeVoteWSCommand(sessionId:String, target:String, initiator:String) extends WebsocketCommand
final case class RevealCardWSCommand(sessionId:String,cardId:Int,initiator:String) extends WebsocketCommand
final case class LeaveGameWSCommand(sessionId:String, initiator:String) extends WebsocketCommand


object WebsocketActor {
  def apply(notifierActor : ActorRef[NotifyCommand],
            sessionManager:ActorRef[SessionManagerCommand | SessionCommand]) : Behavior[WebsocketCommandWithReply] = Behaviors.receive{(ctx,msg) => {
    ctx.log.info("Websocket new message " + msg.command)
    val replyTo = msg.replyTo
    msg.command match
      case ReserveNewConnection(gId, sId) =>
        notifierActor ! AddConnection(gId, replyTo)
        sessionManager ! SessionCommandProxy(sId, AddGuestPlayer(notifierActor, gId))
        Behaviors.same
      case StartGameWSCommand(sId, uId) =>
        sessionManager ! SessionCommandProxy(sId, StartGame(notifierActor, uId))
        Behaviors.same
      case MakeVoteWSCommand(sId, trg, init) =>
        sessionManager ! SessionCommandProxy(sId, VoteForPlayer(notifierActor, trg, init))
        Behaviors.same
      case RevealCardWSCommand(sId, cId, init) =>
        sessionManager ! SessionCommandProxy(sId, RevealCard(notifierActor, init, cId))
        Behaviors.same
      case LeaveGameWSCommand(sId, init) =>
        sessionManager ! SessionCommandProxy(sId, LeaveGame(notifierActor, init))
        Behaviors.same
  }
}}


def JsonToWebsocketCommand(data:Json): Option[WebsocketCommand] = {

  implicit val decodeLong: Decoder[Long] = Decoder.decodeString.emap { s =>
    scala.util.Try(s.toLong).toEither.left.map(_ => s"Invalid Long: $s")
  }
  implicit val decodeInt: Decoder[Int] = Decoder.decodeString.emap { s =>
    scala.util.Try(s.toInt).toEither.left.map(_ => s"Invalid Int: $s")
  }


  def parseReserveConnection(cursor: HCursor): Either[io.circe.DecodingFailure, ReserveNewConnection] = {
    for {
       gId <- cursor.downField("data").downField("id").as[String]
       sId <- cursor.downField("data").downField("sessionId").as[String]
     } yield ReserveNewConnection(gId, sId)
  }

  def parseStartGame(cursor: HCursor): Either[io.circe.DecodingFailure, StartGameWSCommand] = {
    for {
      init <- cursor.downField("data").downField("id").as[String]
      sId <- cursor.downField("data").downField("sessionId").as[String]
    } yield StartGameWSCommand(sId, init)
  }

  def parseVoteCommand(cursor: HCursor): Either[io.circe.DecodingFailure, MakeVoteWSCommand] = {
    for {
      init <- cursor.downField("data").downField("id").as[String]
      sId <- cursor.downField("data").downField("sessionId").as[String]
      trg <- cursor.downField("data").downField("targetId").as[String]
    } yield MakeVoteWSCommand(sId, trg, init)
  }

  def parseRevealCardCommand(cursor: HCursor): Either[io.circe.DecodingFailure, RevealCardWSCommand] = {
    for {
      init <- cursor.downField("data").downField("id").as[String]
      sId <- cursor.downField("data").downField("sessionId").as[String]
      cId <- cursor.downField("data").downField("cardId").as[Int]
    } yield RevealCardWSCommand(sId, cId, init)
  }

  def parseLeaveGameCommand(cursor: HCursor): Either[io.circe.DecodingFailure, LeaveGameWSCommand] = {
    for {
      init <- cursor.downField("data").downField("id").as[String]
      sId <- cursor.downField("data").downField("sessionId").as[String]
    } yield LeaveGameWSCommand(sId, init)
  }

  val cursor = data.hcursor
  val operation = cursor.as[String]
  cursor.downField("operationType").as[String] match {
    case Left(er) => None
    case Right(operationString) =>
      val dispatcher = HashMap[String, (HCursor => Either[io.circe.DecodingFailure, WebsocketCommand])](
        ("reserveNewConnection", parseReserveConnection),
        ("startGame", parseStartGame),
        ("voteFor",parseVoteCommand),
        ("revealCard",parseRevealCardCommand),
        ("closeConnection",parseLeaveGameCommand)
      )
      println(dispatcher.contains(operationString))

      dispatcher.find(_._1 == operationString) match
        case None => None
        case Some(fnc) => fnc._2(cursor) match
          case Left(value) => None
          case Right(cmd) =>
            Some(cmd)
  }

}


