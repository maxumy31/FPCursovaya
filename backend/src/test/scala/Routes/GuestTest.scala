package Routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.*
import Directives.*
import Transport.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.typed.scaladsl.Behaviors

class GuestTest extends AnyWordSpec with Matchers with ScalatestRouteTest{

  implicit val system: ActorSystem[MainRequest] = ActorSystem(Behaviors.empty, "testSystem")
  val routes = NewRestRoutes(system)

    "Create guest" should {
      "return unique id" in {
        Get() ~
      }
    }
}
