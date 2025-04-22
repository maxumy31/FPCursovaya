package Transport


import Application.*
import akka.actor.Status.*
import akka.actor.Status
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{PathMatchers, Route}
import akka.http.scaladsl.server.Route.seal
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

def NewRestRoutes(actor: ActorSystem[MainRequest]) : Route = {
  import akka.actor.typed.scaladsl.AskPattern.Askable
  implicit val timeout: Timeout = 10.seconds
  implicit val ec: ExecutionContext = actor.executionContext
  implicit val scheduler: Scheduler = actor.scheduler
  val HATEAOSResp = PrepareHATEAOSResponse("localhost:9090")
  concat(
    path("healthcheck") {
      get {
        complete("alive")
      }
    } ~
      path("guest") {
        post {
          val operation : Future[GuestRegistered] = actor.ask(replyTo => RegisterGuest(replyTo))
          onComplete(operation) {
            case Success(GuestRegistered(data)) => {
              val respString = Json.obj(
                "id" -> data.guestId.asJson
              ).toString
              val resp = HATEAOSResp(respString)
              complete(resp)
            }
            case Failure(ex) => {
              val respString = "error"
              val resp = HATEAOSResp(respString)
              complete(resp)
            }
          }
        }
      } ~
      path("session") {
        post {
          val operation : Future[SessionCreatedResponse] = actor.ask(replyTo => SessionManagerCommandProxy(CreateSession(replyTo)))
          onComplete(operation) {
            case Success(SessionCreatedResponse(id)) =>
              val respString = Json.obj(
                "id" -> id.toString.asJson
              ).toString
              val resp = HATEAOSResp(respString)
              complete(resp)

            case Failure(ex) =>
              val respString = "error"
              val resp = HATEAOSResp(respString)
              complete(resp)
          }
        }
      } ~
      path("session" / IntNumber) { id => {
        delete {
          complete(s"session $id must be deleted")
        } ~
          get {
            val operation : Future[SessionCreatedResponse] = actor.ask(replyTo => SessionManagerCommandProxy(CreateSession(replyTo)))
            onComplete(operation) {
              case Success(SessionCreatedResponse(id)) =>
                val respString = Json.obj(
                  "id" -> id.toString.asJson
                ).toString
                val resp = HATEAOSResp(respString)
                complete(resp)

              case Failure(ex) =>
                val respString = "error"
                val resp = HATEAOSResp(respString)
                complete(resp)
          }
          }
      }})
}


val websocketFlow: Flow[Message, Message, Any] = Flow[Message].collect {
  case TextMessage.Strict(text) =>
    println(s"Received message: $text")
    TextMessage(s"Echo: $text")
}

val webSocket : Route = path("ws") {
  handleWebSocketMessages(websocketFlow)
}

def PrepareHATEAOSResponse(serverURL:String)(text:String):String = {
  final case class Link(rel:String,href:String,method:String)
  final case class Response(data:String,links:Seq[Link],errors:Option[String])
  val links : Seq[Link] = Seq(
    Link("Register guest",serverURL+"/guest","POST"),
    Link("Check guest",serverURL+"/guest","GET"),
    Link("Register session",serverURL+"/session","POST"),
    Link("Check session",serverURL+"/session","GET"),
    Link("Delete session",serverURL+"/session","DELETE"),
    Link("Add player to session",serverURL+"/session/{sessionID}","POST"),
  )
  val response = Response(text,links,None)
  Json.obj(
    "data" -> text.asJson,
    "links" -> links.asJson
  ).toString

}
