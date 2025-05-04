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
final case class IdForSessionCreated(replyTo: ActorRef[SessionCreatedResponse], id:Long) extends SessionManagerCommand
final case class CheckSessionExists(replyTo:ActorRef[SessionManagerResponse], id:Long) extends SessionManagerCommand
final case class DeleteSession(replyTo:ActorRef[SessionAccessResponse], id:Long) extends SessionManagerCommand
final case class SessionCommandProxy(sessionId : Long,sessionCommand: SessionCommand) extends SessionManagerCommand

sealed trait SessionManagerResponse
final case class SessionCreatedResponse(id: Long) extends SessionManagerResponse
final case class SessionAccessResponse(actorId: Long) extends SessionManagerResponse
object SessionNotFoundResponse extends SessionManagerResponse

object SessionDeletedResponse extends SessionManagerResponse

object SessionManagerActor  {
  implicit val timeout: Timeout = 3.seconds

  def apply(SessionIdGenerator: ActorRef[GenerateId])
           (map: HashMap[Long, ActorRef[SessionCommand]]): Behavior[SessionManagerCommand | SessionCommand] =
    Behaviors.receive { (ctx, msg) => {
      msg match
        case CreateSession(from) =>
          val IdGeneratedAdapter: ActorRef[IdGenerated] = ctx.messageAdapter(rsp => IdForSessionCreated(from, rsp.data))
          SessionIdGenerator ! GenerateId(IdGeneratedAdapter)
          Behaviors.same

        case IdForSessionCreated(replyTo, id) =>
          ctx.log.info(s"New session with id $id registered")
          val newSessionRef = ctx.spawn(SessionActor(WaitingState(Seq()))(SessionService), s"Session$id")
          replyTo ! SessionCreatedResponse(id)
          this (SessionIdGenerator)(map.updated(id, newSessionRef))

        case Application.Actors.CheckSessionExists(replyTo, id) =>
          map.find(_._1 == id) match
            case Some((actorId,ref)) => replyTo ! SessionAccessResponse(actorId)
            case None => replyTo ! SessionNotFoundResponse
          Behaviors.same

        case Application.Actors.DeleteSession(_, _) => ???
        case SessionCommandProxy(id,cmd) => 
          map(id) ! cmd
          Behaviors.same
    }
    }
}

