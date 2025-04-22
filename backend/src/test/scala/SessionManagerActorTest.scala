
import Application.{CreateSession, SessionManagerActor}
import Transport.*
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuite, AnyFunSuiteLike}

import scala.concurrent.duration.FiniteDuration
import org.scalatest.wordspec.AnyWordSpec
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.immutable.{HashMap, HashSet}

class SessionManagerActorTest extends AnyFunSuiteLike with BeforeAndAfterAll {
  private val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("Generate unique IDs until capacity") {
    val manager = testKit.spawn(SessionManagerActor(HashMap()))
    manager ! CreateSession
    /*val a = 84589L
    val b = 45989L
    val c = 217728L
    val initialLast = 0L

    val generator = testKit.spawn(
      IdGeneratorActor(a, b, c)(HashSet.empty, initialLast))
    val probe = testKit.createTestProbe[Long]()

    val ids = (1 to 1000).map { _ =>
      generator ! IdGenerateRequest(probe.ref)
      probe.receiveMessage(new FiniteDuration(1,java.util.concurrent.TimeUnit.SECONDS))
    }

    assert(ids.distinct.size == 1000)*/
  }

}