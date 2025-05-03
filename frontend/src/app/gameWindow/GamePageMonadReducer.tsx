import React from 'react';
import { eitherT, readonlyArray, state } from 'fp-ts';
import { pipe } from 'fp-ts/function';
import * as WS from '@/app/Websocket'
import * as E from 'fp-ts/lib/Either';
import * as TE from 'fp-ts/lib/TaskEither';
import { serialize } from 'v8';
import * as IO from 'fp-ts/lib/IO';
import * as O from "fp-ts/lib/Option"
import * as T from 'fp-ts/lib/Task';
import { map } from 'fp-ts/lib/EitherT';
import * as S from 'fp-ts/lib/State';
import { useStateMonad } from './StateMonadHook';
import { JSX } from 'react';
import { ReadonlyURLSearchParams } from 'next/navigation';
import { EstablishConnection, WebsocketCommands } from './WebsocketManipulations';
import { match, P } from 'ts-pattern';


export type UserConnectionData = {
    userId :string
    sessionId:string
}

export type GameState = {
    players : string[]
    gameStarted : boolean
}

export type PageState = 
{
  type: "initState",
  args : O.Option<UserConnectionData>
}|
{
    type:"failedConnection"
}|
{
    type:"waitingConnectionAsync",
    playerData:UserConnectionData,
    clientConnection:TE.TaskEither<string,WS.WebSocketClient>
} | {
  type:"waitingGame",
  "host":boolean,
  "gameState":GameState,
  "playerData":UserConnectionData
} | {
    type:"activeGame",
    "host":boolean,
    "gameState":GameState,
    "playerData":UserConnectionData
}


export function TransitFailedState() : S.State<PageState, Promise<E.Either<JSX.Element, JSX.Element>>> {
    const ui = <div>failed</div>
    const state : PageState = {
        type:"failedConnection"
    }
    const second = new Promise((resolve,reject) => resolve(E.right(ui)))
    return () => [second, state] as [Promise<E.Either<JSX.Element,JSX.Element>>, PageState]
}




export function ParseUserDataAction(): S.State<PageState, TE.TaskEither<JSX.Element, JSX.Element>> {
    const URL = "ws://localhost:9090/ws";

    const stateFunction = (oldState: PageState): [TE.TaskEither<JSX.Element, JSX.Element>, PageState] => {
        const successUI = <div>Waiting for connection!</div>;
        const errorUI = <div>Kuda connect delat</div>;

        type ContextA = {
            sessionId: string;
            userId: string;
            client?: WS.WebSocketClient;
        }

        type ContextB = {
            sessionId: string;
            userId: string;
            client: TE.TaskEither<string,WS.WebSocketClient>
        }

        const IsInit = (state: PageState): state is { 
            type: 'initState', 
            args: O.Option<UserConnectionData> 
        } => state.type === 'initState';

        const ConnectionDataToContext = (conData: UserConnectionData): ContextA => ({
            sessionId: String(conData.sessionId),
            userId: String(conData.userId)
        });

        const ConnectToWebsocket = (ctx:ContextA) : ContextB => {
            const a =  pipe(
                WS.NewWebsocketClient(URL),
                (te) => {
                    const newCtx : ContextB = {
                        sessionId: ctx.sessionId,
                        userId: ctx.userId,
                        client: te,
                    }
                    return newCtx
                }
                )
            return a
        }

        if (IsInit(oldState)) {
            const result = pipe(
                oldState,
                (curState) => pipe(curState.args, O.map(ConnectionDataToContext)),
                O.map(ConnectToWebsocket),
                O.fold(
                    () => {
                        return [TE.left(errorUI), oldState] as [TE.TaskEither<JSX.Element, JSX.Element>, PageState]
                    },
                    (a : ContextB) => {
                            const userData : UserConnectionData = {
                                userId : a.userId,
                                sessionId : a.sessionId
                            }
                            const newState :PageState = {
                                    type:"waitingConnectionAsync",
                                    playerData: userData,
                                    clientConnection:a.client
                            }
                            return [TE.right(successUI), newState] as [TE.TaskEither<JSX.Element, JSX.Element>, PageState];
                        }
                )
            )
            return result
        } else {
            return [TE.left(errorUI), oldState] as [TE.TaskEither<JSX.Element, JSX.Element>, PageState]
        }
    }
    return stateFunction;
}