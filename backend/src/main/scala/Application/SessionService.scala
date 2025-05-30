package Application

import Application.Actors.*
import Domain.*
import Domain.CardType.{Apokalipsis, Bunker, Profession, Threat}



sealed trait ISessionService:
  val LastRound = 4
  val MaxPlayers = 8
  def apply(state:GameState, command : SessionCommand) : GameState
  def AddPlayerService(state: GameState,id : String):GameState
  def TransferNextState(state: GameState, shuffleSeed : Int) : GameState
  def VotePlayer(state:GameState, target:String, from : String) : GameState
  def RevealCard(state:GameState, card:Int, id:String) : GameState
  def DeletePlayerService(state:GameState, whoToDelete:String) : GameState
  def StartGame(state:GameState, initiator : String, shuffleSeed:Int) : GameState

object SessionService extends ISessionService{

  def AddPlayerService(state: GameState, id: String): GameState = {
    state match
      case WaitingState(lobby) =>
        if(lobby.contains(id) || lobby.length >= MaxPlayers) {
          state
        } else {
          WaitingState(lobby.appended(id))
        }
      case _ => state
  }

  def TransferNextState(state: GameState, shuffleSeed:Int): GameState = {
    def MostFrequent(items: Seq[String]): Seq[String] = {
      if (items.isEmpty) return Seq.empty

      val frequencyMap = items.groupBy(identity).view.mapValues(_.size)
      val maxFrequency = frequencyMap.values.maxOption.getOrElse(0)

      frequencyMap
        .collect { case (num, count) if count == maxFrequency => num }
        .toSeq
        .sorted
    }

    def RevealFirstUnrevealedCard(cards: Seq[(Card, Boolean)]): Seq[(Card, Boolean)] = {
      val unrevealedIndex = cards.indexWhere(!_._2)
      if (unrevealedIndex == -1) then cards else {
        val (before, after) = cards.splitAt(unrevealedIndex)
        val updatedCard = (after.head._1, true)
        before ++ (updatedCard +: after.tail)
      }
    }

    def RevealAllDeck(cards : Seq[(Card,Boolean)]) : Seq[Card] = cards.map((c,b) => c)

    def GenerateThreats(cards: Deck, left : Int) : Seq[Card] = {
      val (newThreat,newDeck) = TakeFirstCardOfType(cards,Threat)
      if(left == 0) {
        newThreat match
          case Some(c) => Seq(c)
          case None => Seq()
      } else {
        newThreat match
          case Some(c) => Seq(c) ++ GenerateThreats(newDeck,left-1)
          case None => Seq()
      }
    }
    
    state match {
      case WaitingState(lobby) => WaitingState(lobby)
      case VotingState(playersAndVotes, rnd, apok, bunker) =>
        val voted = playersAndVotes.map { case (id, voteOpt, deck) =>
          voteOpt match
            case Some(vote) => 1
            case None => 0
        }.sum

        if (voted == playersAndVotes.length) {
          val votes = playersAndVotes.flatMap { case (id, voteOpt, deck) => voteOpt }
          val toKick = MostFrequent(votes).head
          val newPlayers = playersAndVotes.filter { case (l, _, _) => l != toKick }.map { case (l, _, d) => (l, d) }
          val newBunker = RevealFirstUnrevealedCard(bunker)

          if (rnd + 1 >= LastRound) {
            GameEnded(
              newPlayers.map { case (l, d) => (l, d.map { case (c, b) => c }) },
              apok,
              newBunker,
              GenerateThreats(ShuffleDeck(TestDeck(),shuffleSeed),newPlayers.length)
            )
          } else {
            PlayingState(rnd + 1, 0, newPlayers, apok, newBunker)
          }
        } else {
          VotingState(playersAndVotes, rnd, apok, bunker)
        }

      case GameEnded(winners,apok,bunker,threats) =>
        GameEnded(winners,apok,bunker,threats)

      case PlayingState(rnd, trn, players,apok,bunker) =>
        if(trn >= players.length) {
          def transit(l:String,s:Seq[(Card,Boolean)]) : (String,Option[String],Seq[(Card,Boolean)]) = {
            (l,None,s)
          }
          VotingState(players.map(transit),rnd,apok,bunker)
        } else {
          PlayingState(rnd,trn,players,apok,bunker)
        }
    }
  }

  def VotePlayer(state:GameState, target:String, from : String) : GameState = {
    state match
      case VotingState(playersAndVotes,rnd,apok,bunker) =>
        if(playersAndVotes.exists((l, o, d) => l == target)) {
          val host = playersAndVotes.find((l, o, d) => l == from)
          val newVotes = playersAndVotes.map(
            (l, o, d) => if (l == from) {
              (l, Some(target), d)
            } else {
              (l, o, d)
            })
          VotingState(newVotes,rnd,apok,bunker)
        } else {
          VotingState(playersAndVotes,rnd,apok,bunker)
        }
      case _ => state

  }

  def RevealCard(state:GameState, card:Int, id:String) : GameState = {
    state match
      case PlayingState(rnd,trn,players,apok,bunker) =>
        val host = players.find((l,d) => l == id)
        host match
          case Some(pId,cards) =>
            val maxIndex = cards.length-1
            if(card > maxIndex) {
              state
            } else {
              val newCards = cards.zipWithIndex.map((data) => {
                if(data._2 == card) then {
                  (data._1._1,true)
                } else {
                  (data._1._1,data._1._2)
                }
              })
              val newPlayers = players.map(
                playerData => {
                  if(playerData._1 == pId) {
                    (pId,newCards)
                  } else {
                    (playerData)
                  }
                }
              )

              PlayingState(rnd,trn+1,newPlayers,apok,bunker)
            }
          case None => state
      case _ => state
  }

  def DeletePlayerService(state:GameState, whoToDelete:String) : GameState = {
    state match
      case VotingState(playersAndVotes,rnd,apok,bunker) =>
        val newPlayers = playersAndVotes.flatMap((l,o,d) => if(l == whoToDelete) then None else Some(l,o,d))
        VotingState(newPlayers,rnd,apok,bunker)
      case PlayingState(_,_,_,_,_) => state
      case GameEnded(_,_,_,_) => state
      case WaitingState(players) =>
        WaitingState(players.flatMap(l => if( l == whoToDelete) then None else Some(l)))
  }

  def StartGame(state:GameState, initiator : String,shuffleSeed:Int) : GameState = {
    def GenerateDeckForPlayer(deck : Deck,left:Int) : Seq[Seq[Card]] = {
      val (set,newDeck) = GeneratePlayerSet(deck)
      if(left <= 0) {
        Seq(set)
      } else {
        GenerateDeckForPlayer(newDeck,left-1) ++ Seq(set)
      }
    }

    def PrepareBunkerCards(deck:Deck,count:Int) : Seq[Card] = {
      val (card,newDeck) = TakeFirstCardOfType(deck,Bunker)
      if(count <= 0) then {
          card match
            case Some(c) => Seq(c)
            case None => Seq()
        } else {
          card match
            case Some(c) => Seq(c) ++ PrepareBunkerCards(newDeck,count)
            case None => Seq()
        }
    }

    def RevealTypeCard(seq : Seq[Card],t : CardType) : Seq[(Card,Boolean)] = {
      seq.map(
        c => {
          if(c.cardType == t) {
            (c,true)
          } else {
            (c,false)
          }
        }
      )
    }

    state match
      case WaitingState(players) =>
        players.headOption match
          case Some(host) =>
            if host == initiator && players.length >= 4  then {
              val deck = TestDeck()
              val shuffledDeck = ShuffleDeck(deck,shuffleSeed)
              val playersDecks = GenerateDeckForPlayer(shuffledDeck,players.length)
              val apokalipsisCard = TakeFirstCardOfType(deck,Apokalipsis)._1.get
              val zippedCards = players.zip(playersDecks).map((id,card) => (id,RevealTypeCard(card,Profession)))
              PlayingState(1,0,zippedCards,apokalipsisCard,
                PrepareBunkerCards(shuffledDeck,5).map(c => (c,false)))
            } else state
          case None => state
      case _ => state
  }

  override def apply(state: GameState, command: SessionCommand): GameState = ???
}