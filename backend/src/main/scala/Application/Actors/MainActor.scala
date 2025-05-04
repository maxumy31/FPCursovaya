package Application.Actors

import Application.*
import Domain.*
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import akka.util.Timeout

import scala.collection.immutable.{HashMap, HashSet}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

sealed trait MainRequest
final case class RegisterGuest(replyTo:ActorRef[GuestRegistered]) extends MainRequest
final case class IdForGuestGenerated(id:Long,replyTo:ActorRef[GuestRegistered]) extends MainRequest

final case class WebsocketMessageProxy(replyTo:ActorRef[String],command:WebsocketCommand) extends MainRequest
final case class SessionManagerCommandProxy(command: SessionManagerCommand) extends MainRequest

sealed trait MainResponse
final case class GuestRegistered(data:GuestUser) extends MainResponse

object MainActor {
  implicit val timeout: Timeout = 3.seconds
  def apply():Behavior[MainRequest] = Behaviors.setup{setupCtx => {
    setupCtx.log.info("Spawning MainActor")

    //https://en.wikipedia.org/wiki/Linear_congruential_generator
    //MMIX by Donald Knuth
    //Брал волшебные цифры оттуда
    val GuestIdGenerator = setupCtx.spawn(IdGeneratorActor(6364136223846793005L,1442695040888963407L,Long.MaxValue)
      (HashSet(),0),"GuestIDGenerator")

    val SessionManager : ActorRef[SessionManagerCommand | SessionCommand] = setupCtx.spawn( Behaviors.setup{ctx => {
      val SessionIdGenerator = ctx.spawn(IdGeneratorActor(6364136223846793005L,1442695040888963407L,Long.MaxValue)
        (HashSet(),0),"SessionIdGenerator")
      SessionManagerActor(SessionIdGenerator)(HashMap())
    }},"SessionManager")

    val Notifier : ActorRef[NotifyCommand] = setupCtx.spawn(NotifierActor(HashMap()),"NotifierActor")

    val WebsocketManager : ActorRef[WebsocketCommandWithReply] = setupCtx.spawn(WebsocketActor(Notifier,SessionManager),"WebsocketManager")

  Behaviors.receive{ (ctx,msg) => {
    msg match
      case RegisterGuest(replyTo) => {
        val IdGeneratedAdapter : ActorRef[IdGenerated] = ctx.messageAdapter(rsp => IdForGuestGenerated(rsp.data,replyTo))
        GuestIdGenerator ! GenerateId(IdGeneratedAdapter)
        Behaviors.same
      }
      case IdForGuestGenerated(id,replyTo) => {
        val guest = GuestUser(id)
        ctx.log.info(s"Guest registered with id = $id")
        replyTo ! GuestRegistered(guest)
        Behaviors.same
      }


      case SessionManagerCommandProxy(command) => {
        SessionManager ! command
        Behaviors.same
      }
      case WebsocketMessageProxy(replyTo, message) => {
        WebsocketManager ! WebsocketCommandWithReply(replyTo,message)
        Behaviors.same
      }
  }}
}}}
