package Application.Actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import cats.data.Reader

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.Base64
import scala.annotation.tailrec
import scala.collection.immutable.{HashMap, HashSet}

trait IdGeneratorCommand
case class GenerateId(from: ActorRef[IdGenerated]) extends IdGeneratorCommand
case class ForgetId(id:String) extends IdGeneratorCommand
case class IdGenerated(data:String)

object IdGeneratorActor {
  val LifeTimeInHours = 24
  val readTime: Reader[Unit, LocalDateTime] = Reader(_ => LocalDateTime.now())
  def apply(a:Long,b:Long,c:Long)(used:HashMap[String,LocalDateTime], last:Long): Behavior[IdGeneratorCommand] =
  Behaviors.receive {(ctx,msg) => {
    val LongGenerator = GenerateLong(a,b,c)
    msg match
      case GenerateId(from) =>
        val currentTime = readTime.run(())
        val (id,newSeed) = GenerateUniqueString(used,last,LongGenerator,currentTime)
        val expireTime = currentTime.plusHours(LifeTimeInHours)
        from ! IdGenerated(id)
        ctx.log.info(s"Generated id $id")
        this(a, b, c)(used + (id -> expireTime), newSeed)
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
def GenerateUniqueString(used:HashMap[String,LocalDateTime], seed:Long, genLong : Long => Long, currentTime : LocalDateTime):(String,Long) = {
  def GenerateString(seed:Long): (String,Long) = {
    val next1 = genLong(seed)
    val next2 = genLong(next1)
    (LongsToString(next1, next2),next2)
  }
  val (id,newSeed) = GenerateString(seed)
  if(used.contains(id) && used(id).isAfter(currentTime)) {
    GenerateUniqueString(used,newSeed,genLong,currentTime)
  } else {
    (id,newSeed)
  }
}