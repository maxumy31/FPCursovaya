package Application

import Domain.*
import Domain.CardType.*


import scala.util.Random

val StandartDeck : Deck = {
  def genCard(t:CardType,left:Int):Seq[Card] = {
    if left == 0 then Seq(Card(t,"test" + left.toInt)) else {
      genCard(t,left-1) ++ Seq(Card(t,"test" + left.toInt))
    }
  }
  val plDeck = genCard(Profession,10) ++
    genCard(Biology,10) ++
    genCard(Hobby,10) ++
    genCard(Fact,10) ++
    genCard(Item,10) ++
    genCard(Health,10)

  val spDeck = genCard(Apokalipsis,10) ++
    genCard(Bunker,10) ++
    genCard(Threat,10)
  Deck(plDeck ++ spDeck)
}


def ShuffleDeck(random:Random,deck:Deck):Deck = {
  val shuffledCards = random.shuffle(deck.cards)
  Deck(shuffledCards)
}


def FindCard(cardType: CardType, deck:Deck) : Option[Card]  = {
  deck.cards.find(c => c.cardType == cardType)
}

def FindCardId(cardType: CardType, deck: Deck): Option[Int] = {
  val id = deck.cards.indexWhere((c:Card) => c.cardType == cardType)
  if id == -1 then None else Some(id)
}

def MarkCardAsUsed(cardType: CardType,deck:Deck,marks:Seq[Boolean]) : (Card,Seq[Boolean]) = {
  val id = deck.cards.zip(marks).indexOf((c:Card,marked:Boolean) => c.cardType == cardType && !marked)
  (deck.cards(id), (0 to deck.cards.length).map(i => if i == id then true else false))
}

def MarkCardAsUsed(cardType: CardType, deck: Deck): (Card, Seq[Boolean]) = {
  val id = deck.cards.indexOf((c: Card) => c.cardType == cardType)
  (deck.cards(id), (0 to deck.cards.length).map(i => if i == id then true else false))
}


def CreatePlayerDeck(deck:Deck):Option[(Seq[Card],Deck)] = {
  val cardTypes = Seq(Hobby,Biology,Health,Hobby,Item,Fact)
  var newCards = deck.cards
  var found : Seq[Card] = Seq()
  val d = cardTypes.map(t => {
    FindCardId(t,Deck(newCards)) match
      case Some(id) => {
        val foundCard = newCards(id)
        newCards = newCards.patch(id,Seq(),1)
        found = found.appended(foundCard)
        Some(foundCard)
      }
      case None => None
  })
  val isValid = d.forall(d => d match
    case None => false
    case Some(_) => true
  )

  if isValid then Some(found,Deck(newCards)) else None
}