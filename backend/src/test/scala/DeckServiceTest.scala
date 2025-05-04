import Application.*
import Domain.*
import Domain.CardType.*
import org.scalatest.*
import flatspec.*
import matchers.*
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class DeckServiceTest extends AnyFunSuite {

  test("Deck shuffler must be consistent") {
    val deck1 = StandardDeck
    val deck2 = StandardDeck
    assert(ShuffleDeck(new Random(42),deck1) == ShuffleDeck(new Random(42),deck2))
  }

  test("Deck must give same cards") {
    val deck1 = ShuffleDeck(new Random(42), StandardDeck)
    val deck2 = ShuffleDeck(new Random(42), StandardDeck)
    assert(FindCard(Bunker,deck1) == (FindCard(Bunker,deck2)))
  }

  test("Deck must select player hands") {
    val deck1 = ShuffleDeck(new Random(42), StandardDeck)
    val deck2 = ShuffleDeck(new Random(42), StandardDeck)
    val (res1) = CreatePlayerDeck(deck1)
    val (res2) = CreatePlayerDeck(deck2)
    assert(res1 == res2)
    res1 match {
      case Some(seq, newDeck) => assert(newDeck.cards.length < deck1.cards.length)
      case None => println("empty deck")
    }
    res2 match
      case Some(seq,newDeck) => {
        CreatePlayerDeck(newDeck) match
          case Some(seq2,newDeck2) =>
            assert(seq2 != seq)
          case None => println("empty deck")
      }
      case None => println("empty deck")

  }


}