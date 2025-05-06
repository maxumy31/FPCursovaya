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
import * as TO from "fp-ts/lib/TaskOption"
import * as T from 'fp-ts/lib/Task';
import { map } from 'fp-ts/lib/EitherT';
import * as S from 'fp-ts/lib/State';
import { ParseMessage, WebsocketCommands } from "./WebsocketManipulations";
import Reduce, { GameAction, GameState } from './GameStateReducer';


const URL = "ws://localhost:9090/ws"



export type UserConnectionData = {
  sessionId:string,
  userId:string,
}

//Парсим параметры из строки
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

  //Из параметров подключения создаем начальную страницу. Если они есть, то пишем что подключение
  //Если нету, то пишем что ошибка
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

  //Главный элемент управления состоянием страницы. Функция Reduce обрабатывает все вызовы
  //к actionReducer и изменяет состояние currentState, за счет чего происходит ререндер
  //ParamsToInitialState(parsedParams) - начальное состояние
  const [currentState,actionReducer] = useReducer(Reduce,ParamsToInitialState(parsedParams))
  

  const ConnectWS = (URL:string,parsedParams:O.Option<UserConnectionData>) => {
    //Пытаемся законектится
    pipe(
      parsedParams,
      O.map(conData => {
        WS.NewWebsocketClient(URL)().then(
          opt => AfterWSConnection(opt)
        )
      })
    )
  }

  //Если получилось создать вебоскет
  const AfterWSConnection = (eith: O.Option<WS.WebSocketClient>) => {
    switch(eith._tag)
    {
    case 'None':
      const actionL :GameAction = {
        type:"CannotConnect"
      }
      actionReducer(actionL)
      break
    case 'Some':
      const actionR : GameAction = {
        type:"ConnectedWS",
        client:eith.value
      }
      actionReducer(actionR)
      break
    }
  }

  //Пытаемся зарезервировать соединение с сервером
  const ReserveConnectionWithWS = (ws:WS.WebSocketClient,connection : UserConnectionData) => {
    const msg : WebsocketCommands =  {
      "operationType":"reserveNewConnection",
      "data" : {
        "id":connection.userId,
        "sessionId":connection.sessionId,
      }
    }
    console.log("Sending :", JSON.stringify(msg))
    ws.Send(JSON.stringify(msg))()
  
    const a = pipe(
      ws.Read(),
      TO.map(msg => {
        const json = JSON.parse(msg)
        const players = json["players"]
        const newCommand :GameAction = {
          type:"ConnectedToWaitingState",
          players:players
        }
        return newCommand
      }),
      TO.map(cmd => {
        actionReducer(cmd)
      })
    )
    a()
  }

  //Читаем сообщения и обновляем состояния пока ждем начала игры
  const PollMessages = (ws:WS.WebSocketClient)=> {
    const a = pipe(
      ws.Read(),
      TO.map(msg => {
        pipe(
          ParseMessage(msg),
          O.map((gsm) => {
            console.log("gsm :",gsm)
            const newAction :GameAction = {
              type:"UpdateCurrentState",
              data:gsm
            }
            actionReducer(newAction)
            PollMessages(ws)
          })
        )
        
      }),
    )
    a()

  }

  //Вызывается при изменении currentState
  useEffect(() => {
    switch(currentState.type)
    {
      case 'EmptyState':
      case 'FailedToConnect':
        break
      case 'WaitingForConnection':
        ConnectWS(URL,parsedParams)
        break
      case 'WaitingForState':
        ReserveConnectionWithWS(currentState.connection,currentState.connectionData)
        break
      case 'GameState':
        PollMessages(currentState.connection)
        break
    }
  },[currentState])


  return(<>
    {currentState.markup}
  </>)
}
