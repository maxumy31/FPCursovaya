package Application

import Domain.*

def CreateStartGameState(playersTotal:Int) : GameState = {
  WaitingState(Seq())
}