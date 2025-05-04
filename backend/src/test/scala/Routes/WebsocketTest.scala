package Routes

import Application.*
import Application.Actors.*
import Transport.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.testkit.WSProbe

class WebSocketRouteSpec extends AnyWordSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem(Behaviors.empty, "testSystem")
  "WebSocket service" should {
    "connect, send message, and receive response" in {
      val wsProbe = WSProbe()(system)
    }
  }
}
