package Domain

case class GameState(round:Int, turn:Int, playersTotal:Int,
                     playersLeft:Int, map: Map[IUser,Deck])
