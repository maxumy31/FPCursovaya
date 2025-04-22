package Application

import Domain.*

def CreateStartGameState(playersTotal:Int) = {
  GameState(0,0,playersTotal,playersTotal,Map())
}