package Domain

import Domain.CardType.*

enum CardType:
  case Profession
  case Biology
  case Health
  case Hobby
  case Item
  case Fact
  case Special
  
  case Apokalipsis
  case Bunker
  case Threat

case class Card(cardType:CardType, description:String)

//Test cards
val ProfessionCard = Card(Profession,"Profession test")
val BiologyCard = Card(Biology,"Biology test")
val HealthCard = Card(Health,"Health test")
val HobbyCard = Card(Hobby,"Hobby test")
val ItemCard = Card(Item,"Item test")
val FactCard = Card(Fact,"Fact test")
val SpecialCard = Card(Special,"Special test")

val ApokalipsisCard = Card(Apokalipsis,"Apokalipsis test")
val BunkerCard = Card(Bunker,"Bunker test")
val ThreatCard = Card(Threat,"Threat test")
