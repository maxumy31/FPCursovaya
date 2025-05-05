package Application

import Domain.*
import Domain.CardType.*


import scala.util.Random


def ShuffleDeck(deck: Deck, seed: Long): Deck = {
  val random = new Random(seed)
  val shuffled = random.shuffle(deck.cards)
  Deck(shuffled)
}

def GeneratePlayerSet(deck: Deck): (List[Card], Deck) = {
  val requiredTypes = List(
    Profession,
    Biology,
    Health,
    Hobby,
    Item,
    Fact
  )

  requiredTypes.foldLeft((List.empty[Card], deck)) {
    case ((collected, currentDeck), cardType) =>
      TakeFirstCardOfType(currentDeck, cardType) match {
        case (Some(card), newDeck) => (collected :+ card, newDeck)
        case (None, newDeck) => (collected, newDeck)
      }
  }
}

def TakeFirstCardOfType(deck: Deck, cardType: CardType): (Option[Card], Deck) = {
  deck.cards.indexWhere(_.cardType == cardType) match {
    case -1 => (None, deck)
    case index =>
      val (before, after) = deck.cards.splitAt(index)
      val newCards = before ++ after.tail
      (Some(after.head), Deck(newCards))
  }
}
