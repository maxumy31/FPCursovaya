package Domain



case class PlayingState(
                      round:Int,
                      turn:Int,
                      players : Seq[(String,Seq[(Card,Boolean)])],
                      apokalipsisCard: Card,
                      bunkerCards : Seq[(Card,Boolean)]
                    )

case class WaitingState(
                         players : Seq[String],
                         )

case class VotingState(
                      //Пояснение к playersAndVotes
                      //Первая строка - айди
                      //Option[String] - голоса(на каждом итом месте голос итого игрока против игрока k-ого)
                      //Seq[Card] - набор карт игрока
                 playersAndVotes : Seq[(String,Option[String],Seq[(Card,Boolean)])],
                 turn : Int,
                 apokalipsisCard: Card,
                 bunkerCards : Seq[(Card,Boolean)]
                 )


case class GameEnded(
                    winners: Seq[(String,Seq[Card])],
                    apokalipsisCard: Card,
                    bunkerCards : Seq[(Card,Boolean)],
                    threats : Seq[Card]
)

type GameState = PlayingState | WaitingState | VotingState | GameEnded