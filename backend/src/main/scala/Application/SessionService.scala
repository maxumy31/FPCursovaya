package Application

import Application.Actors.*
import Domain.*







/*def StartGame(state:GameState,id:Long,deck: Deck) :GameState = {
  def CreateDeck(times:Int,initialDeck : Deck) : Seq[Option[]]= {
    CreatePlayerDeck(initialDeck) match {
      
    }
  }
  state match {
    case WaitingState(lobby) => if(lobby.head == id) {
      
      PlayingState(0,0,players)
    } else {
      state
    }
  }
}*/

/*def DeletePlayerService(state:GameState,deleteId :Long): GameState = {
  state match
    case WaitingState(lobby) =>
      WaitingState(lobby.filter(el => el != deleteId))
    case VotingState(players, votes) =>
      VotingState(players.filter(el => el != deleteId), votes)
    case PlayingState(round, turn, players) =>
      val newPlayers = players.map {
        el => {
          el match
            case (l:Long,d:Deck) => if (l == deleteId) then () else (l,d)
            case _ => ()
        }
      }
      PlayingState(round, turn, newPlayers)
}
*/
sealed trait ISessionService:
  val LastRound = 5
  def apply(state:GameState, command : SessionCommand) : GameState
  def AddPlayerService(state: GameState,id : Long):GameState
  def TransferNextState(state: GameState) : GameState
  def VotePlayer(state:GameState, target:Long, from : Long) : GameState
  def RevealCard(state:GameState, card:Int, id:Long) : GameState
  def DeletePlayerService(state:GameState, whoToDelete:Long) : GameState
  def StartGame(state:GameState, initiator : Long) : GameState

object SessionService extends ISessionService{

  def AddPlayerService(state: GameState, id: Long): GameState = {
    state match
      case WaitingState(lobby) =>
        WaitingState(lobby.appended(id))
      case _ => state
  }

  def TransferNextState(state: GameState): GameState = {
    def mostFrequent(items: Seq[Long]): Seq[Long] = {
      if (items.isEmpty) return Seq.empty

      val frequencyMap = items.groupBy(identity).view.mapValues(_.size)
      val maxFrequency = frequencyMap.values.maxOption.getOrElse(0)

      frequencyMap
        .collect { case (num, count) if count == maxFrequency => num }
        .toSeq
        .sorted
    }
    
    state match {
      case WaitingState(lobby) => WaitingState(lobby)
      case VotingState(playersAndVotes,rnd) =>
        if(rnd == LastRound) then {
          GameEnded(playersAndVotes.map((l,o,d) => (l,d)))
        }
        val voted = playersAndVotes.map((id,voteOpt,deck) => {
          voteOpt match
            case Some(vote) => 1
            case None => 0
        }).sum
        if (voted == playersAndVotes.length) then {
          val votes = playersAndVotes.map((id,voteOpt,deck) => {
            voteOpt match
              case Some(vote) => vote
          })
          val toKick = mostFrequent(votes).head
          val newPlayers = playersAndVotes.filter((l,o,d) => l != toKick).map((l,o,d) => (l,d))
          PlayingState(rnd,0,newPlayers)
        } else {
          VotingState(playersAndVotes,rnd)
        }
      case GameEnded(winners) => 
        GameEnded(winners)

      case PlayingState(rnd, trn, players) =>
        if(trn >= players.length) {
          def transit(l:Long,d:Deck) : (Long,Option[Long],Deck) = {
            (l,None,d)
          }
          VotingState(players.map(transit),rnd)
        } else {
          PlayingState(rnd,trn,players)
        }
    }
  }

  def VotePlayer(state:GameState, target:Long, from : Long) : GameState = {
    state match
      case VotingState(playersAndVotes,rnd) =>
        if(playersAndVotes.exists((l, o, d) => l == target)) {
          val host = playersAndVotes.find((l, o, d) => l == from)
          val newVotes = playersAndVotes.map(
            (l, o, d) => if (l == from) {
              (l, Some(target), d)
            } else {
              (l, o, d)
            })
          VotingState(newVotes,rnd)
        } else {
          VotingState(playersAndVotes,rnd)
        }
      case _ => state

  }

  def RevealCard(state:GameState, card:Int, id:Long) : GameState = {
    state match
      case PlayingState(rnd,trn,players) =>
        val host = players.find((l,d) => l == id)
        host match
          case Some(_,deck) =>
            val maxIndex = deck.cards.length-1
            if(card > maxIndex) {
              state
            } else {
              val newDeck = Deck(deck.cards,
                deck.revealed.zipWithIndex.map(
                  (b,i) => if(i == card) then {true} else {b}))
              val newPlayers = players.map((plId,plDeck) => {
                if(plId == id) then (plId,newDeck) else (plId,plDeck)
              })
              PlayingState(rnd,trn,players)
            }
          case None => state
      case _ => state
  }

  def DeletePlayerService(state:GameState, whoToDelete:Long) : GameState = {
    state match
      case VotingState(playersAndVotes,rnd) =>
        val newPlayers = playersAndVotes.flatMap((l,o,d) => if(l == whoToDelete) then None else Some(l,o,d))
        VotingState(newPlayers,rnd)
      case PlayingState(_,_,_) => state
      case GameEnded(_) => state
      case WaitingState(players) =>
        WaitingState(players.flatMap(l => if( l == whoToDelete) then None else Some(l)))
  }

  def StartGame(state:GameState, initiator : Long) : GameState = {
    def GenerateDeckForPlayer(deck : Deck,left:Int) : Seq[Deck] = {
      val (set,newDeck) = GeneratePlayerSet(deck)
      if(left <= 0) {
        Seq(Deck(set,set.map(c => false)))
      } else {
        GenerateDeckForPlayer(newDeck,left-1) ++ Seq(Deck(set,set.map(c => false)))
      }
    }

    state match
      case WaitingState(players) =>
        players.headOption match
          case Some(host) =>
            if host == initiator then {
              val deck = TestDeck()
              val shuffledDeck = ShuffleDeck(deck,42)
              val playersDecks = GenerateDeckForPlayer(shuffledDeck,players.length)
              PlayingState(0,0,players.zip(playersDecks))
            } else state
          case None => state
  }

  override def apply(state: GameState, command: SessionCommand): GameState = ???
}