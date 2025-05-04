import org.scalatest.funsuite.AnyFunSuite
import Application.*
import Application.Actors.GenerateId
import Domain.*
import Domain.CardType.*
import org.scalatest.*
import flatspec.*
import matchers.*
import org.scalatest.funsuite.AnyFunSuite

import akka.actor.*
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.duration.FiniteDuration


class GuestRegisterActorClassicTest extends TestKit(ActorSystem("TestSystem")) 
  with ImplicitSender with AnyFunSuiteLike with BeforeAndAfterAll {
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)


}

