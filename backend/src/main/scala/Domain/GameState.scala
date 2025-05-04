package Domain



case class PlayingState(
                      round:Int,
                      turn:Int,
                      players : Seq[(String,Deck)]
                    )

case class WaitingState(
                         players : Seq[String],
                         )

case class VotingState(
                 playersAndVotes : Seq[(String,Option[String],Deck)],
                 turn : Int
                 )


case class GameEnded(
                    winners: Seq[(String,Deck)]
)

type GameState = PlayingState | WaitingState | VotingState | GameEnded