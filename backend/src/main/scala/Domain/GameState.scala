package Domain



case class PlayingState(
                      round:Int,
                      turn:Int,
                      players : Seq[(Long,Deck)]
                    )

case class WaitingState(
                         players : Seq[Long],
                         )

case class VotingState(
                 playersAndVotes : Seq[(Long,Option[Long],Deck)],
                 turn : Int
                 )


case class GameEnded(
                    winners: Seq[(Long,Deck)]
)

type GameState = PlayingState | WaitingState | VotingState | GameEnded