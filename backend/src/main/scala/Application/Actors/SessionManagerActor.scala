package Application.Actors

import Application.*
import Domain.*
import Transport.*
import akka.actor.Actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.collection.immutable.{HashMap, HashSet}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

sealed trait SessionManagerCommand
final case class CreateSession(replyTo:ActorRef[SessionCreatedResponse]) extends SessionManagerCommand
final case class IdForSessionCreated(replyTo: ActorRef[SessionCreatedResponse], id:String) extends SessionManagerCommand
final case class CheckSessionExists(replyTo:ActorRef[SessionManagerResponse], id:String) extends SessionManagerCommand
final case class DeleteSession(replyTo:ActorRef[SessionAccessResponse], id:String) extends SessionManagerCommand
final case class SessionCommandProxy(sessionId : String,sessionCommand: SessionCommand) extends SessionManagerCommand

sealed trait SessionManagerResponse
final case class SessionCreatedResponse(id: String) extends SessionManagerResponse
final case class SessionAccessResponse(actorId: String) extends SessionManagerResponse
object SessionNotFoundResponse extends SessionManagerResponse

object SessionDeletedResponse extends SessionManagerResponse

object SessionManagerActor  {
  implicit val timeout: Timeout = 3.seconds

  def apply(SessionIdGenerator: ActorRef[GenerateId])
           (map: HashMap[String, ActorRef[SessionCommand]]): Behavior[SessionManagerCommand | SessionCommand] =
    Behaviors.receive { (ctx, msg) => {
      msg match
        case CreateSession(from) =>
          val IdGeneratedAdapter: ActorRef[IdGenerated] = ctx.messageAdapter(rsp => IdForSessionCreated(from, rsp.data))
          SessionIdGenerator ! GenerateId(IdGeneratedAdapter)
          Behaviors.same

        case IdForSessionCreated(replyTo, id) =>
          ctx.log.info(s"New session with id $id registered")
          val newSessionRef = ctx.spawnAnonymous(SessionActor(WaitingState(Seq()),42)(SessionService))
          replyTo ! SessionCreatedResponse(id)
          this (SessionIdGenerator)(map.updated(id, newSessionRef))

        case Application.Actors.CheckSessionExists(replyTo, id) =>
          map.find(_._1 == id) match
            case Some((actorId,ref)) => replyTo ! SessionAccessResponse(actorId)
            case None => replyTo ! SessionNotFoundResponse
          Behaviors.same

        case SessionCommandProxy(id,cmd) =>
          val contains = map.contains(id)
          if(contains)  {
            ctx.log.info("Using command",cmd,"for id =",id)
            map(id) ! cmd
            Behaviors.same
          } else {
            Behaviors.same
          }


    }
    }
}

