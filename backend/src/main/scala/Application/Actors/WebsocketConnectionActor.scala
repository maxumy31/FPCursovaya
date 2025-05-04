package Application.Actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Flow, Source}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration._


sealed trait WebsocketConnectionCommand


object WebsocketConnectionActor {

  def apply(clientRef: ActorRef[String], clientId: Long,
            sessionId: Long, sessionManager : ActorRef[SessionManagerCommand | SessionCommand]): Behavior[String] =
    Behaviors.setup { context =>
      context.log.info(s"Actor created for client: $clientId, session: $sessionId")

      Behaviors.withTimers { timers =>
        //timers.startTimerAtFixedRate("timer-key", "timer", 1.second)

        Behaviors.receiveMessage {
          case message@"timer" =>
            clientRef ! message
            Behaviors.same

          case other =>
            context.log.info(s"Received message: $other")
            Behaviors.same
        }
      }
    }
}