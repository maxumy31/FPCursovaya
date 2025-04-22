package Transport

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpRequest
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

object MarshallerActor {
  def apply():Behavior[HttpRequest] = {
    Behaviors.receive{(ctx,msg) => {
      Behaviors.same
    }}
  }
}
