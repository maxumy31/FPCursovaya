package Domain

case class UserCards(profession: Card,
                     biology : Card,
                     health:Card,
                     hobby : Card,
                     item:Card,
                     fact:Card
                    )

def CreateTestUserCardsSet() : UserCards = {
  UserCards(ProfessionCard, BiologyCard, HealthCard, HobbyCard, ItemCard, FactCard)
}