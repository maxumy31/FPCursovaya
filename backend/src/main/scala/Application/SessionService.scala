package Application

import Domain.*

def CreateStartingSession(playersTotal:Int) = {
  Session(Seq(),CreateStartGameState(playersTotal))
}

def AddUserToSession(user:IUser,ses:Session) : Session = {
  Session(ses.users.appended(user),ses.gameState)
}