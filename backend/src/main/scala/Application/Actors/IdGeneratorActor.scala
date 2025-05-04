package Application.Actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.util.Base64
import scala.annotation.tailrec
import scala.collection.immutable.HashSet

trait IdGeneratorCommand
case class GenerateId(from: ActorRef[IdGenerated]) extends IdGeneratorCommand
case class ForgetId(id:String) extends IdGeneratorCommand
case class IdGenerated(data:String)

object IdGeneratorActor {
  def apply(a:Long,b:Long,c:Long)(used:HashSet[String],last:Long): Behavior[IdGeneratorCommand] =
  Behaviors.receive {(ctx,msg) => {
    val LongGenerator = GenerateLong(a,b,c)
    msg match
      case GenerateId(from) =>
        val next1 = LongGenerator(last)
        val next2 = LongGenerator(next1)
        val next3 = LongGenerator(next2)
        val next4 = LongGenerator(next3)
        val id = LongsToString(next1,next2,next3,next4)
        from ! IdGenerated(id)
        ctx.log.info(s"Generated id $id")
        this(a, b, c)(used.incl(id), next4)

      case ForgetId(id) =>
        ctx.log.info(s"Deleting id $id")
        this(a, b, c)(used.excl(id), last)
  }}
}

def GenerateLong(a:Long, b:Long, c:Long)(seed :Long):Long = {
  (seed * a + b) % c
}

def LongsToString(a:Long,b:Long,c:Long,d:Long):String = {
  val bytes = ByteBuffer.allocate(32)
  val encoder = Base64.getEncoder.withoutPadding()
  bytes.putLong(a)
  bytes.putLong(b)
  bytes.putLong(c)
  bytes.putLong(d)
  encoder.encodeToString(bytes.array())
}