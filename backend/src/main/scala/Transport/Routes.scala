package Transport

import Application.Actors.*
import akka.actor.Status.*
import akka.actor.Status
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.model.headers.{`Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives.{onComplete, *}
import akka.http.scaladsl.server.{PathMatchers, Route}
import akka.http.scaladsl.server.Route.seal
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.util.Timeout
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.stream.OverflowStrategy

def NewRestRoutes(actor: ActorSystem[MainRequest]) : Route = {

  implicit val timeout: Timeout = 10.seconds
  implicit val ec: ExecutionContext = actor.executionContext
  implicit val scheduler: Scheduler = actor.scheduler
  val HATEAOSResp = PrepareHATEAOSResponse("localhost:9090")
  corsHandler(
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
              val respJSON = Json.obj(
                "id" -> data.guestId.asJson
              )
              val resp = HATEAOSResp(Right(respJSON))
              complete(resp)
            }
            case Failure(ex) => {
              val respJSON = "error"
              val resp = HATEAOSResp(Left(respJSON))
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
              val respJSON = Json.obj(
                "id" -> id.toString.asJson
              )
              val resp = HATEAOSResp(Right(respJSON))
              complete(resp)

            case Failure(ex) =>
              val respJSON = "error"
              val resp = HATEAOSResp(Left(respJSON))
              complete(resp)
          }
        }
      } ~
      path("session" / LongNumber) { id => {
        delete {
          complete(s"session $id must be deleted")
        } ~
          get {
            println(id)
            val operation : Future[SessionManagerResponse] = actor.ask(replyTo => SessionManagerCommandProxy(CheckSessionExists(replyTo,id)))
            onComplete(operation) {
              case Success(SessionAccessResponse(sesId)) =>
                val respJSON = Json.obj(
                  "id" -> sesId.toString.asJson
                )
                val resp = HATEAOSResp(Right(respJSON))
                complete(resp)

              case Success(SessionNotFoundResponse) =>
                val respJSON = "Session not found".asJson
                val resp = HATEAOSResp(Right(respJSON))
                complete(resp)

              case Failure(ex) =>
                val respJSON = "error"
                val resp = HATEAOSResp(Left(respJSON))
                complete(resp)
          }
          }
      }})
}

def adapterBehavior(queue: SourceQueueWithComplete[Message]): Behavior[String] =
  Behaviors.setup { context =>
    Behaviors.receiveMessage { msg =>
      println("output : " + msg)
      queue.offer(TextMessage.Strict(msg))
      Behaviors.same
    }
  }

def NewWebsocketFlow(actor:ActorSystem[MainRequest]):Flow[Message, Message, Any] = {
  implicit val timeout: Timeout = 10.seconds
  implicit val ec: ExecutionContext = actor.executionContext
  implicit val scheduler: Scheduler = actor.scheduler
  implicit val system: ActorSystem[MainRequest] = actor

  val (queue, source) = Source
    .queue[Message](bufferSize = 100, OverflowStrategy.dropHead)
    .preMaterialize()
  
  val adapter = actor.systemActorOf(adapterBehavior(queue), "websocket-adapter" + java.util.UUID.randomUUID())
  println("Websocket connection established")
  Flow.fromSinkAndSource(
    Flow[Message]
      .collect {
        case TextMessage.Strict(text) => text
        case TextMessage.Streamed(x) => "Unsupported"
      }
      .to(Sink.foreach { text =>
        println(text)
        parse(text) match
          case Left(err) => println("Wrong parsed")
          case Right(value) =>
            JsonToWebsocketCommand(value) match
              case Some(cmd) => system ! WebsocketMessageProxy(adapter, cmd)
              case None => println("No commands")

      }),

    source
  )
}

def NewWebsocketRoute(actor:ActorSystem[MainRequest]):Route = {
  val webSocket: Route = path("ws") {
    handleWebSocketMessages(NewWebsocketFlow(actor))
  }
  webSocket
}

def PrepareHATEAOSResponse(serverURL: String)(responseData: Either[String,Json]): String = {
  final case class Link(rel: String, href: String, method: String)
  final case class Response(data: Json, links: Seq[Link], errors: Option[String] = None)

  val links = Seq(
    Link("Register guest", s"$serverURL/guest", "POST"),
    Link("Check guest", s"$serverURL/guest", "GET"),
    Link("Register session", s"$serverURL/session", "POST"),
    Link("Check session", s"$serverURL/session", "GET"),
    Link("Delete session", s"$serverURL/session", "DELETE"),
    Link("Add player to session", s"$serverURL/session/{sessionID}", "POST")
  )
  responseData match {
    case Left(str) => Response("".asJson, links, Some(str)).asJson.noSpaces
    case Right(data) => Response(data, links, None).asJson.noSpaces
  }

}

def corsHandler(route: Route): Route = {
  respondWithHeaders(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
    `Access-Control-Allow-Headers`("Content-Type", "Authorization")
  ) {
    options {
      complete("")
    } ~
      route
  }
}
