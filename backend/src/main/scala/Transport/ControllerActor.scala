package Transport

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.sun.org.slf4j.internal.LoggerFactory

case class Request(request:HttpRequest, from:ActorRef[HttpResponse])
object ControllerActor {

  def apply():Behavior[Request] = {
    Behaviors.setup{setupCtx => {
      val marshaller = 10
    }
    Behaviors.receive{(ctx,msg) => {
      println(msg.request)
      println(msg.request.entity)
    }
      val response = msg.request.uri.path.toString match {
        case "/api/hello" => HttpResponse(entity = "hello back")
        case _ => HttpResponse(404)
      }
      msg.from ! response
      Behaviors.same
    }}
  }
}
