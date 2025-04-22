import org.scalatest.funsuite.AnyFunSuite
import Application.*
import Domain.*
import Domain.CardType.*
import org.scalatest.*

import scala.collection.immutable.HashSet
import scala.util.Random

class GuestServiceTest extends AnyFunSuite {
  test("must create unique users") {
    val id1 = CreateNewUserId(new Random(42),HashSet[Long]())
    val newSet = HashSet(id1)
    val id2 = CreateNewUserId(new Random(42),newSet)
    assert(id1 != id2)
  }
}
