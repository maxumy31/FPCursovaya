import {GameState,UserConnectionData, PageState, ParseUserDataAction} from "@/app/gameWindow/GamePageMonadReducer"
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
import { useStateMonad } from './StateMonadHook';



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
    "votingForId":string,
  }
} 






  export const SendCommand = (command: WebsocketCommands, client : WS.WebSocketClient):TE.TaskEither<string,void> => {
    console.log("sending")
    return client.Send(JSON.stringify(command))
  }


  

  export const EstablishConnection = (clientPromise : TE.TaskEither<string,WS.WebSocketClient>, 
    userData : O.Option<UserConnectionData>): TE.TaskEither<string,void>  => {

      const TryConnectToSession = (userParams : UserConnectionData, client : WS.WebSocketClient):TE.TaskEither<string,void> => {
        const connectionMessage : WebsocketCommands = {
          operationType:"reserveNewConnection",
          data : {
            "id": String(userParams.userId),
            "sessionId" : String(userParams.sessionId)
          }
        }
        return SendCommand(connectionMessage,client)
      }

      const userDataEither = E.fromOption(() => "UserData is missing")(userData)

      return pipe(
        clientPromise,
        TE.chain(client => 
          pipe(
            userDataEither,
            TE.fromEither,
            TE.chain(userParams => TryConnectToSession(userParams,client))
          )
        )
      )
  }