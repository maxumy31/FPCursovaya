package Application

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.annotation.tailrec
import scala.collection.immutable.HashSet

trait IdGeneratorCommand
case class GenerateId(from: ActorRef[IdGenerated]) extends IdGeneratorCommand
case class ForgetId(id:Long) extends IdGeneratorCommand
case class IdGenerated(data:Long)

object IdGeneratorActor {
  def apply(a:Long,b:Long,c:Long)(used:HashSet[Long],last:Long): Behavior[IdGeneratorCommand] =
  Behaviors.receive {(ctx,msg) => {
    msg match
      case GenerateId(from) =>
        val next = GenerateUniqueId(a, b, c)(used, last)
        from ! IdGenerated(next)
        ctx.log.info(s"Generated id $next")
        this (a, b, c)(used.incl(next), next)

      case ForgetId(id) =>
        ctx.log.info(s"Deleting id $id")
        this (a, b, c)(used.excl(id), last)
  }}
}

@tailrec
def GenerateUniqueId(a:Long, b:Long, c:Long)(used:HashSet[Long], seed :Long):Long = {
  val next = (seed * a + b) % c
  if used.contains(next) then {
    GenerateUniqueId(a,b,c)(used,next)
  } else {
    next
  }
}