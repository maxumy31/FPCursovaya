package Application

import Domain.*
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{Actor, ActorRef}


//sealed trait SessionCommand

/*object SessionActor {
  def apply(): Behavior[SessionCommand] = 
    Behaviors.setup { (setupCtx) => {
      setupCtx.log.info("New session created")
    }
      Behaviors.receive{(ctx,msg) => {
        Behaviors.same
      }}
}}
*/