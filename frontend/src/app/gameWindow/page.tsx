'use client'

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
import * as T from 'fp-ts/lib/Task';
import { map } from 'fp-ts/lib/EitherT';
import * as S from 'fp-ts/lib/State';
import { WebsocketCommands } from "./WebsocketManipulations";
import Reduce, { GameAction, GameState } from './GameStateReducer';


const URL = "ws://localhost:9090/ws"



export type UserConnectionData = {
  sessionId:string,
  userId:string,
}

const ParseParams = (params:ReadonlyURLSearchParams | null) : O.Option<UserConnectionData> => {
    return pipe(
      params,
      O.fromNullable,
      O.flatMap(searchParams => {
          const data = searchParams.get('data');
          const parsedData = data ? JSON.parse(data) : null;
          if(parsedData) {
            return O.some(parsedData)
          } else {
            return O.none
          }
        })
      )
  }

  const ParamsToInitialState = (conData : O.Option<UserConnectionData>): GameState => {
    const state:O.Option<GameState> =  pipe(
      conData,
      O.map((conData : UserConnectionData) => {
          const initState : GameState = {
            type:"WaitingForConnection",
            markup:<div>Подключаемся...</div>,
            connectionData:conData
          }
          return initState
        }
      )
    )
    const emptyState : GameState = {
      type:"EmptyState",
      markup:<div>Произошла ошибка. Нету данных для подключения</div>
    }
    return state._tag == "Some" ? state.value : emptyState

  }



export default function GameWindows(){

  //Парсим данные со строки
  const params = useSearchParams()
  const parsedParams = ParseParams(params)

  //Если есть куда конектится, то пытаемся. Если нет, то забиваем
  const [currentState,actionReducer] = useReducer(Reduce,ParamsToInitialState(parsedParams))

  const ConnectWS = (URL:string,parsedParams:O.Option<UserConnectionData>) => {
    //Пытаемся законектится
    pipe(
      parsedParams,
      O.map(conData => {
        WS.NewWebsocketClient(URL)().then(
          eith => AfterWSConnection(eith)
        )
      })
    )
  }


  const AfterWSConnection = (eith:E.Either<string,WS.WebSocketClient>) => {
    switch(eith._tag)
    {
    case 'Left':
      const actionL :GameAction = {
        type:"CannotConnect"
      }
      actionReducer(actionL)
      break
    case 'Right':
      const actionR : GameAction = {
        type:"ConnectedWS",
        client:eith.right
      }
      actionReducer(actionR)
      break
    }
  }


  const ReserveConnectionWithWS = (ws:WS.WebSocketClient,connection : UserConnectionData) => {
    const msg : WebsocketCommands =  {
      "operationType":"reserveNewConnection",
      "data" : {
        "id":connection.sessionId,
        "sessionId":connection.userId,
      }
    }
    ws.Send(JSON.stringify(msg))()
    
  }

  useEffect(() => {

    

  })

  useEffect(() => {
    switch(currentState.type)
    {
      case 'EmptyState':
        break
      case 'FailedToConnect':
        break
      case 'WaitingForConnection':
        ConnectWS(URL,parsedParams)
        break
      case 'WaitingForState':
        ReserveConnectionWithWS(currentState.connection,currentState.connectionData)
        break
    }
  },[currentState])


  return(<>
    {currentState.markup}
  </>)
}
