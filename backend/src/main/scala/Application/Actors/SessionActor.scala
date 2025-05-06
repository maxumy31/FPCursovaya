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
            val stateBefore = state
            val stateAfterVoting = sessionService.VotePlayer(state, trg,init)
            val stateAfter = stateAfterVoting
            val nextState = sessionService.TransferNextState(stateAfterVoting)
            val lostPlayer = GetLostPlayer(GetPlayers(stateBefore),GetPlayers(nextState))
            ctx.log.error(lostPlayer.toString)
            lostPlayer match
              case Some(pl) => ntf ! PlayerKicked(pl)
              case None =>
            ntf ! NotifyWith(nextState)
            this (nextState)(sessionService)
      }}
}

def GetLostPlayer(before:Seq[String], after:Seq[String]) : Option[String] = {
      val beforeSet = before.toSet
      val afterSet = after.toSet
      println(beforeSet.toString + afterSet.toString)
      val lostPlayers = beforeSet -- afterSet
      lostPlayers.headOption
}

def GetPlayers(state:GameState) : Seq[String] = {
  state match
    case VotingState(pl,_,_,_) => pl.map((l) => l._1)
    case PlayingState(_,_,pl,_,_) => pl.map((l) => l._1)
    case _ => Seq()
}