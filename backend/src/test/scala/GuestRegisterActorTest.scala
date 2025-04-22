import org.scalatest.funsuite.AnyFunSuite
import Application.*
import Domain.*
import Domain.CardType.*
import org.scalatest.*
import flatspec.*
import matchers.*
import org.scalatest.funsuite.AnyFunSuite
import Transport.GuestRegisterActor
import Transport.GenerateId
import akka.actor.*
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.duration.FiniteDuration


class GuestRegisterActorClassicTest
  extends TestKit(ActorSystem("TestSystem"))
    with ImplicitSender
    with AnyFunSuiteLike
    with BeforeAndAfterAll {
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
  test("Generate valid and unique UUID") {
    val generator = system.actorOf(Props[GuestRegisterActor]())

    def genId(left:Int):Seq[String] = {
      generator ! GenerateId
      val res = expectMsgType[String](new FiniteDuration(1, java.util.concurrent.TimeUnit.SECONDS))
      if left == 0 then Seq(res) else {
        Seq(res) ++ genId(left-1)
      }
    }
    val ids = genId(1000)
    assert(ids.distinct.length == ids.length)
  }

  test("Do not generate UUID if not GenerateID") {
    val generator = system.actorOf(Props[GuestRegisterActor]())
    generator ! 123
    val res = expectNoMessage()
  }
}

