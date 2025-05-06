
import { eitherT, readonlyArray, state } from 'fp-ts';
import { useRef, useEffect, useMemo, useState, JSX, useReducer } from 'react'
import { pipe } from 'fp-ts/function';
import * as WS from '@/app/Websocket'
import * as E from 'fp-ts/lib/Either';
import * as TE from 'fp-ts/lib/TaskEither';
import { ReadonlyURLSearchParams, useParams, useSearchParams } from 'next/navigation';
import { serialize } from 'v8';
import * as IO from 'fp-ts/lib/IO';
import * as O from "fp-ts/lib/Option"
import { Task } from 'fp-ts/lib/Task';
import { map } from 'fp-ts/lib/EitherT';
import * as S from 'fp-ts/lib/State';
import { error } from 'console';



export type WebsocketCommands = 
 {
  "operationType":"reserveNewConnection",
  data : {
    "id":string,
    "sessionId":string,
  }
} | 
 {
  "operationType":"revealCard",
  data : {
    "id":string,
    "sessionId":string,
    "cardId":string,
  }
} | 
 {
  "operationType":"startGame",
  data : {
    "id":string,
    "sessionId":string,
  }
} |
 {
  "operationType":"voteFor",
  data : {
    "id":string,
    "targetId":string,
    "sessionId":string
  }
} |
{
  "operationType":"voteFor",
  "data" : {
    "id":string,
    "sessionId":string
  }
} 

type CardType = 
  | "Profession" 
  | "Biology" 
  | "Health" 
  | "Hobby" 
  | "Item" 
  | "Fact" 
  | "Apokalipsis" 
  | "Bunker";

export type PlayersWithReveableCards = {
  "id":string,
  "cards":ReveableCard[]
}

export type ReveableCard = {
  "cardType":string,
  "description":string,
  "isRevealed":boolean
}

export type PlayerWithVotesAndCards = {
  "id":string,
  "voted":string|null,
  "cards":ReveableCard[]
}

export type GameStateMessage = {
  "type":"waitingState",
  "players":string[]
} | {
  "type":"playingState",
  "round":number,
  "turn":number,
  "players":PlayersWithReveableCards[],
  "apokalipsis":ReveableCard,
  "bunkerCards":ReveableCard[]
} | {
  "type":"votingsState",
  "players":PlayerWithVotesAndCards[],
  "round":number,
  "apokalipsis":ReveableCard,
  "bunkerCards":ReveableCard[]
} | {
  "type":"playerKicked"
} | {
  "type":"gameEnded",
  "players":PlayersWithReveableCards[],
  "apokalipsis":ReveableCard,
  "bunkerCards":ReveableCard[],
  "threats":ReveableCard[]
}

export const PrepareConnectionMessage = (userId:string,sessionId:string):WebsocketCommands => {
  return {
    "operationType":"reserveNewConnection",
    "data" : {
      "id":userId,
      "sessionId":sessionId,
    }
  }
}

export const PrepareRevealCardMessage = (userId:string,cardId:number,sessionId:string):WebsocketCommands => {
  return {
    "operationType":"revealCard",
    "data" : {
      "id":userId,
      "sessionId":sessionId,
      //Да. Поле явно число, но если прислать число на скалу, то цирк(circe) не сможет ее распарсить :3
      "cardId":String(cardId),
    }
  }
}

export const PrepareStartGameMessage = (userId:string,sessionId:string):WebsocketCommands => {
  return  {
    "operationType":"startGame",
    "data" : {
      "id":userId,
      "sessionId":sessionId,
    }
  }
}

export const PrepareVotingMessage = (userId:string,sessionId:string,targetId:string):WebsocketCommands => {
  return   {
    "operationType":"voteFor",
    data : {
      "id":userId,
      "targetId":targetId,
      "sessionId":sessionId
    }
  } 
}

export const PrepareLeavingMessage = (userId:string,sessionId:string):WebsocketCommands => {
  return  {
    "operationType":"voteFor",
    "data" : {
      "id":userId,
      "sessionId":sessionId
    }
  } 
}

export const ParseMessage = (message:any):O.Option<GameStateMessage> => {
  return O.tryCatch(
    () => {
      const json = JSON.parse(message)
      const msgType:string = json["type"]
      switch(msgType) {
        case "waitingState":
          const players : string[] = json["players"]!
          const waitingState : GameStateMessage = {
            "type":msgType,
            "players":players
          }
          return waitingState
          case "playingState":
            return parseGameStateMessage(json)
          case "votingState":
            return transformToVotingsState(json)
          case "playerKicked":
            return {type:"playerKicked"}
          case "gameEnded":
            return parseEndState(json)

      }
      throw new Error("Cannot parse")
    }
  )
}





export function parseGameStateMessage(json: any): GameStateMessage {
  if (typeof json !== "object" || json === null) {
    throw new Error("Invalid JSON structure");
  }

  const parseReveableCard = (cardData: any): ReveableCard => {
    if (!Array.isArray(cardData) || cardData.length !== 2) {
      throw new Error("Invalid card format");
    }
    
    const [card, isRevealed] = cardData;
    const cardTypeKey = Object.keys(card.cardType)[0];
  
    return {
      cardType: cardTypeKey as CardType,
      description: card.description,
      isRevealed: Boolean(isRevealed)
    };
  };

  const parsePlayers = (playersData: any[]): PlayersWithReveableCards[] => {
    return playersData.map(playerData => {
      if (!Array.isArray(playerData) || playerData.length !== 2) {
        throw new Error("Invalid player format");
      }
      
      const [id, cardsData] = playerData;
      return {
        id: String(id),
        cards: (cardsData as any[]).map(parseReveableCard)
      };
    });
  };

  const parseBunkerCards = (cardsData: any[]): ReveableCard[] => {
    return cardsData.map(cardData => ({
      ...parseReveableCard(cardData),
      cardType: "Bunker"
    }));
  };

  return {
    type: "playingState",
    round: Number(json.round),
    turn: Number(json.turn),
    players: parsePlayers(json.players),
    apokalipsis: {
      ...parseReveableCard([json.apokalipsisCard, true]),
      cardType: "Apokalipsis"
    },
    bunkerCards: parseBunkerCards(json.bunkerCards)
  };
}

export function transformToVotingsState(rawData: any): GameStateMessage {
  const transformCards = (cardsArray: any[]): ReveableCard[] => 
    cardsArray.map(([cardData, isRevealed]) => ({
      cardType: cardData.cardType,
      description: cardData.description,
      isRevealed
    }));

  return {
    type: "votingsState",
    players: rawData.playersAndVotes.map((playerData: any[]) => ({
      id: playerData[0],
      voted: playerData[1],
      cards: transformCards(playerData[2])
    })),
    round: rawData.turn, 
    apokalipsis: {
      cardType: rawData.apokalipsisCard.cardType,
      description: rawData.apokalipsisCard.description,
      isRevealed: true 
    },
    bunkerCards: rawData.bunkerCards.map((card: any) => ({
      ...card,
      isRevealed: true
    }))
  };
}

export function parseEndState(rawData: any): GameStateMessage {
  
  if (rawData.type === 'gameEnded') {
    return {
      type: 'gameEnded',
      players: rawData.winners.map(([id, cards]: [string, any[]]) => ({
        id,
        cards: cards.map(card => ({
          cardType: Object.keys(card.cardType)[0],
          description: card.description,
          isRevealed: true
        }))
      })),
      apokalipsis: {
        cardType: Object.keys(rawData.apokalipsisCard.cardType)[0],
        description: rawData.apokalipsisCard.description,
        isRevealed: true
      },
      bunkerCards: rawData.bunkerCards.map(([card, isRevealed]: [any, boolean]) => ({
        cardType: Object.keys(card.cardType)[0],
        description: card.description,
        isRevealed
      })),
      threats: rawData.threats.map(([card,isRevealed] : [any,boolean]) => ({
        cardType: Object.keys(card.cardType)[0],
        description: card.description,
        isRevealed
      })),
    }
  }


  throw new Error(`Unsupported message type: ${rawData.type}`);
}
