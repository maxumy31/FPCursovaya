package Transport

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse

case class Link(name:String,path:String,method:String)
case class HATEAOSResponse(response:HttpResponse,from:ActorRef[HttpResponse])
object HATEAOSActor {
  def apply(links: Seq[Link]):Behavior[HttpResponse] = Behaviors.receive{
    (ctx,msg) =>
      val copy = HttpResponse(msg.status,msg.headers,msg.entity,msg.protocol)
      Behaviors.same
  }
}
