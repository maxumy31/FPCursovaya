import { act, JSX } from "react"
import * as WS from "../Websocket"
import {UserConnectionData} from "./page"
import WaitingPageState from "./PageStates/WaitingPageState"
import {GameStateMessage, PlayersWithReveableCards} from "./WebsocketManipulations"
import PlayingPageState from "./PageStates/PlayingPageState"
import VotingPageState from "./PageStates/VotingPageState"
import GameEndedState from "./PageStates/GameEndedState"

export type GameAction =  {
    type:"ConnectedWS"
    client:WS.WebSocketClient
} | {
    type:"CannotConnect"
} | {
    type:"ConnectedWithSession"
} | {
    type:"ConnectedToWaitingState",
    players: string[]
} | {
    type:"UpdateCurrentState",
    data:GameStateMessage
}

export type GameState = 
{
    type:"EmptyState"
    markup:JSX.Element
} |{
    type:"FailedToConnect",
    markup:JSX.Element,
} | {
    type:"WaitingForConnection",
    markup:JSX.Element,
    connectionData:UserConnectionData
} | {
    type:"WaitingForState",
    connection:WS.WebSocketClient,
    connectionData:UserConnectionData
    markup:JSX.Element,
} | {
    type:"GameState",
    connection:WS.WebSocketClient,
    connectionData:UserConnectionData,
    markup:JSX.Element,
    data: GameStateMessage
}

//Этот ужас обрабатывает все GameAction и мутирует состояние страницы
export default function Reduce(currentState: GameState, action: GameAction): GameState {
    console.log("Reducer input:", currentState, action);
  
    const baseState = { ...currentState };
    switch (action.type) {
      case "ConnectedWS":
        if (currentState.type === "WaitingForConnection") {
          return {
            type: "WaitingForState",
            connection: action.client,
            markup: <div>Ждем информацию с сервера</div>,
            connectionData: currentState.connectionData
          };
        }
        break;
  
      case "CannotConnect":
        if (currentState.type === "WaitingForConnection") {
          return {
            type: "FailedToConnect",
            markup: <div>Не удалось подключиться</div>
          };
        }
        break;
  
      case "ConnectedToWaitingState":
        if (currentState.type === "WaitingForState") {
            const waitingMsg : GameStateMessage = {
                type:"waitingState",
                players:[currentState.connectionData.userId]
            }
          return {
            connection:currentState.connection,
            connectionData:currentState.connectionData,
            type: "GameState",
            data: waitingMsg,
            markup: WaitingPageState(
              action.players,
              currentState.connectionData.userId,
              currentState.connectionData.sessionId,
              currentState.connection
            )
          };
        }
        break;
  
      case "UpdateCurrentState":
        if (currentState.type === "GameState") {
            if(action.data.type === "waitingState") {
                return {
                    connection:currentState.connection,
                    connectionData:currentState.connectionData,
                    type: "GameState",
                    data: action.data,
                    markup: WaitingPageState(
                      action.data.players,
                      currentState.connectionData.userId,
                      currentState.connectionData.sessionId,
                      currentState.connection
                    )
                  };
            } else if(action.data.type === "playingState") {
                console.log("update to playing state")
                console.log(action.data)
                return {
                    connection:currentState.connection,
                    connectionData:currentState.connectionData,
                    type: "GameState",
                    data: action.data,
                    markup: PlayingPageState(
                        action.data, currentState.connection, 
                        currentState.connectionData.userId,currentState.connectionData.sessionId
                    )
                  };
            } else if(action.data.type === "votingsState") {
              console.log("update to voting state")
                console.log(action.data)
                return {
                    connection:currentState.connection,
                    connectionData:currentState.connectionData,
                    type: "GameState",
                    data: action.data,
                    markup: VotingPageState(
                        action.data, currentState.connection, 
                        currentState.connectionData.userId,currentState.connectionData.sessionId
                    )
                  };
            } else if(action.data.type === "playerKicked") {
              console.log("you was kicked")
                console.log(action.data)
                return {
                    connection:currentState.connection,
                    connectionData:currentState.connectionData,
                    type: "GameState",
                    data: action.data,
                    markup: <div>Вы были изгнаны</div>
                  };
            } else if(action.data.type === "gameEnded") {
              console.log("you was kicked")
                console.log(action.data)
                return {
                    connection:currentState.connection,
                    connectionData:currentState.connectionData,
                    type: "GameState",
                    data: action.data,
                    markup: GameEndedState(action.data,currentState.connection,
                      currentState.connectionData.userId,currentState.connectionData.sessionId)
                  };
                }
          
        }
        break;
    }
  
    console.warn("Unhandled action:", action);
    return { ...currentState };
  }