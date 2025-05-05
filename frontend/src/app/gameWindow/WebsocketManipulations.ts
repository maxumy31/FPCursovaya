
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
  "operationType":"makeMove",
  data : {
    "id":string,
    "sessionId":string,
    "cardId":number,
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
    "operationType":"makeMove",
    "data" : {
      "id":userId,
      "sessionId":sessionId,
      "cardId":cardId,
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
            const a = parseGameStateMessage(json)
            console.log(a)
            return a
      }
      throw new Error("Cannot parse")
    }
  )
}





export function parseGameStateMessage(json: any): GameStateMessage {
  // Валидация базовой структуры
  if (typeof json !== "object" || json === null) {
    throw new Error("Invalid JSON structure");
  }

  // Парсинг карт с флагом раскрытия
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

  // Парсинг игроков
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

  // Парсинг бункерных карт
  const parseBunkerCards = (cardsData: any[]): ReveableCard[] => {
    return cardsData.map(cardData => ({
      ...parseReveableCard(cardData),
      // Для бункерных карт принудительно выставляем тип
      cardType: "Bunker"
    }));
  };

  // Основной парсинг
  return {
    type: "playingState",
    round: Number(json.round),
    turn: Number(json.turn),
    players: parsePlayers(json.players),
    apokalipsis: {
      ...parseReveableCard([json.apokalipsisCard, true]), // Предполагаем что апокалипсис всегда раскрыт
      cardType: "Apokalipsis"
    },
    bunkerCards: parseBunkerCards(json.bunkerCards)
  };
}