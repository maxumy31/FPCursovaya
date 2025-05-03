'use client'
import {GameState,UserConnectionData, PageState, ParseUserDataAction, TransitFailedState} from "@/app/gameWindow/GamePageMonadReducer"
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
import { useStateMonad } from './StateMonadHook';
import { WebsocketCommands } from "./WebsocketManipulations";




  const CreateWaitingPage = (data : O.Option<UserConnectionData>, gameState : GameState) : JSX.Element => {
    const IsHost = (userData:UserConnectionData) : boolean => String(userData.userId) == gameState.players[0]
    return pipe(
      data,
      O.map(user => IsHost(user) ? O.some(user) : O.none),
      O.fold(
        () => {
          return <div>Ждем хоста</div>
        },
        (userData) => {
          return <button>Начать игру</button>
        }
      )

    )
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



export default function GameWindows(){
  const URL = "ws://localhost:9090/ws"

  const searchParams = useSearchParams();
  const parsedParams = ParseParams(searchParams)

  const [page,setPage] = useState<JSX.Element>(<div></div>)
  const initState : PageState = {
    type:"initState",
    args:parsedParams
  }
  
  const [state, runState] = useStateMonad<PageState>(initState)

  const gameState : GameState = {players : ["7076646890315895000"],gameStarted:false}

  const SetWebsocketConnectionCheck = (ws: WS.WebSocketClient) : IO.IO<void> => {
    setTimeout(() => {
      const con : WebsocketCommands = {
        "operationType":"reserveNewConnection",
        "data":{
          "id":"123",
          "sessionId":"123"
        }
      }
      ws.Send(JSON.stringify(con))()
      console.log("Sent")
    }, 1000);
    return (() => {const a = 3})
  }


  const StartListening = (ws:WS.WebSocketClient) : IO.IO<void> => {
    setTimeout(() => {
      ws.Read()().then(msg => {
        switch(msg._tag) {
          case "Left":
            console.log("error")
            break
          case "Right":
            console.log(msg.right)
            break
        }
      })
      StartListening(ws)
    },1000)
    
    return (() => {const a=3})
  }
  
  
  useEffect(() => {
    
    const state : S.State<PageState,JSX.Element> = (state) => {
      switch (state.type) {
        case "initState":
          const a = pipe(
            parsedParams,
            O.map(opt => {
              const client = WS.NewWebsocketClient(URL)
              return client
          }),
          O.map(opt => {
            opt().then(eith => {
              switch(eith._tag) {
                case "Left":
                  console.log("left")
                  setTimeout(() => console.log("timeout"),100)
                  break
                case "Right":
                  const client = eith.right
                  SetWebsocketConnectionCheck(client)
                  StartListening(client)()
              }
            })
          })
        )
          break
      }
      return [<div></div>,state]
    }
    state(initState)
    /*type StateType = S.State<PageState, TE.TaskEither<JSX.Element,JSX.Element>>

    console.log("Before",state)
    const handleAction = async () => {
      try {
        await runState(TransitFailedState());
        console.log("Final state:", state); 
      } catch (error) {
        console.error("Error:", error);
      }
    }
    handleAction()

    const UpdateUI = (page : E.Either<JSX.Element,JSX.Element>): IO.IO<void> => {
      console.log(page)
      return pipe(
        page,
        E.fold(
          (left) => () => {
            setPage(left)},
          (right) => () => {
            setPage(right)}
        ),
      )
    }*/

    /*
    const HandleStateTransition = (
      action: StateType,
      currentState: PageState
    ): [TE.TaskEither<JSX.Element, JSX.Element>, PageState] => {
      const [result, newState] = action(currentState)
      return [result, newState]
    }

    

    /*const TransitAndUpdate = (
      action: StateType,
      currentState: PageState
    ): PageState => {
      const [result, newState] = HandleStateTransition(action,state)
      UpdateUI(result)()
      return newState
    }*/

    /*const [page,nState] = S.get<PageState>()(state)

    const StatePipeline = pipe(
      S.get<PageState>(),
      S.map((curState : PageState) => {
        const [page, newState] = ParseUserDataAction()(curState)
        pipe(
          page,
          TE.fold(
            (left) => {
              setPage(left)
              return T.of(undefined)
            },
            (right) => {
              setPage(right)
              return T.of(undefined)
            }
          )
        )()
        return newState
      }//return TransitAndUpdate(ParseUserDataAction(),state)}),
  
    ))(state)
    
    */

  },[])

  const startButton = <div><button>Начать игру</button></div>

  return(<>
    {page}
  </>)
}
