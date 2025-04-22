package Domain

case class Session(users:Seq[IUser], gameState: GameState)
