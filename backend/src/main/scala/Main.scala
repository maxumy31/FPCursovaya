import Application.*
import Application.Actors.{JsonToWebsocketCommand, MainActor, MainRequest}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route.seal
import Transport.*
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.Flow
import io.circe.*
import io.circe.parser.*
import io.circe.generic.auto.*
import io.circe.syntax.*

import scala.collection.immutable.HashMap

object Main extends App {
  implicit val system: ActorSystem[MainRequest] = ActorSystem(MainActor(), "main")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val timeout: Timeout = 3.seconds


  val restRoutes = NewRestRoutes(system)
  val wsRoutes = NewWebsocketRoute(system)


  val server = Http().newServerAt("localhost", 9090).bind(restRoutes ~ wsRoutes)

  server.map { _ =>
    println("Successfully started on localhost:9090 ")
  } recover {
    case ex =>
      println("Failed to start the server due to: " + ex.getMessage)
  }
}
