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
        val (id,newSeed) = GenerateUniqueString(used,last,LongGenerator)
        from ! IdGenerated(id)
        ctx.log.info(s"Generated id $id")
        this(a, b, c)(used.incl(id), newSeed)

      case ForgetId(id) =>
        ctx.log.info(s"Deleting id $id")
        this(a, b, c)(used.excl(id), last)
  }}
}

def GenerateLong(a:Long, b:Long, c:Long)(seed :Long):Long = {
  (seed * a + b) % c
}

def LongsToString(a:Long,b:Long):String = {
  val bytes = ByteBuffer.allocate(16)
  val encoder = Base64.getEncoder.withoutPadding()
  bytes.putLong(a)
  bytes.putLong(b)
  encoder.encodeToString(bytes.array())
}
@tailrec
def GenerateUniqueString(used:HashSet[String], seed:Long, genLong : Long => Long):(String,Long) = {
  def GenerateString(seed:Long): (String,Long) = {
    val next1 = genLong(seed)
    val next2 = genLong(next1)
    (LongsToString(next1, next2),next2)
  }
  val (newString, nextSeed) = GenerateString(seed)
  if(used.contains(newString)) {
    GenerateUniqueString(used,genLong.andThen(genLong)(seed),genLong)
  } else {
    (newString,nextSeed)
  }
}