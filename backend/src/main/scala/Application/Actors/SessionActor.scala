package Application.Actors

import Application.*
import Domain.*
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors


sealed trait SessionCommand
case class AddGuestPlayer(notifyActor: ActorRef[NotifyCommand], guestId:String) extends SessionCommand
case class LeaveGame(notifyActor: ActorRef[NotifyCommand], guestId:String) extends SessionCommand
case class RevealCard(notifyActor: ActorRef[NotifyCommand], guestId:String,cardId:Int) extends SessionCommand
case class StartGame(notifyActor: ActorRef[NotifyCommand], guestId:String) extends SessionCommand
case class VoteForPlayer(notifyActor: ActorRef[NotifyCommand], target:String, initiator : String) extends SessionCommand



object SessionActor {
  val MaxPlayersPerSession = 16
  def apply(state:GameState)(sessionService: ISessionService): Behavior[SessionCommand] =
      Behaviors.receive{(ctx,msg) => {
        msg match
          case AddGuestPlayer(notify,gId) =>
            val nextState = sessionService.AddPlayerService(state,gId)
            notify ! NotifyWith(nextState)
            this(nextState)(sessionService)
          case LeaveGame(notify,gId) =>
            val nextState = sessionService.DeletePlayerService(state,gId)
            notify ! NotifyWith(nextState)
            this(nextState)(sessionService)
          case RevealCard(ntf,gId,cId) =>
            val nextState = sessionService.TransferNextState(sessionService.RevealCard(state,cId,gId))
            ntf ! NotifyWith(nextState)
            this(nextState)(sessionService)
          case StartGame(ntf,gId) =>
            val nextState = sessionService.StartGame(state,gId)
            ntf ! NotifyWith(nextState)
            this(nextState)(sessionService)
          case VoteForPlayer(ntf,trg,init) =>
            val nextState = sessionService.TransferNextState(sessionService.VotePlayer(state, trg,init))
            ntf ! NotifyWith(nextState)
            this (nextState)(sessionService)
      }}
}
