import { act, JSX } from "react"
import * as WS from "../Websocket"
import {UserConnectionData} from "./page"

export type GameAction =  {
    type:"ConnectedWS"
    client:WS.WebSocketClient
} | {
    type:"CannotConnect"
} | {
    type:"ConnectedWithSession"
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
    type:"WaitingForState"
    connection:WS.WebSocketClient,
    connectionData:UserConnectionData
    markup:JSX.Element,
}


export default function Reduce(currentState : GameState, action : GameAction): GameState {
    switch(currentState.type)
    {
        case "FailedToConnect":
            return currentState
            break
        case "WaitingForConnection":
            switch(action.type)
            {
                case "ConnectedWS":
                    const newState1 : GameState = {
                        type:"WaitingForState",
                        connection:action.client,
                        markup:<div>Ждем информацию с сервера</div>,
                        connectionData : currentState.connectionData
                    }
                    return newState1
                    break
                case "CannotConnect":
                    const newState2 : GameState = {
                        type:"FailedToConnect",
                        markup:<div>Не удалось подключиться</div>
                    }
                    return newState2
                default:
                    return currentState
                    break
            }

        case "WaitingForState":
            return currentState
    }
    console.log("Cannot change state")
    return currentState
    
}