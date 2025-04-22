package Application

import scala.collection.immutable.HashSet
import scala.util.Random


def CreateNewUserId(random: Random, used:HashSet[Long]):Long = {
  val v = random.nextLong()
  if used.contains(v) then CreateNewUserId(random,used) else v
}