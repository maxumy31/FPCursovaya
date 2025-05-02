'use client'
import { eitherT, readonlyArray } from 'fp-ts';
import { State } from 'fp-ts/State';
import { useRef, useEffect, useMemo } from 'react'
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

type WebsocketCommands = 
{
  operationType:"reserveNewConnection",
  data : {
    "id":string,
    "sessionId":string,
  }
} | 
{
  operationType:"makeMove",
  data : {
    "id":string,
    "sessionId":string,
    "cardId":string,
  }
}


type UserConnectionData = {
  userId :BigInt
  sessionId:BigInt
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

  const SendCommand = (command: WebsocketCommands, client : WS.WebSocketClient):TE.TaskEither<string,void> => {
    console.log("sending")
    return client.Send(JSON.stringify(command))
  }


  

  const EstablishConnection = (clientPromise : TE.TaskEither<string,WS.WebSocketClient>, 
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



export default function GameWindows(){
  const URL = "ws://localhost:9090/ws"

  const searchParams = useSearchParams();
  
  
  useEffect(() => {
    const clientPromise : TE.TaskEither<string,WS.WebSocketClient> =  WS.NewWebsocketClient(URL)
    const parsedParams = ParseParams(searchParams)
    EstablishConnection(clientPromise,parsedParams)()
  })

  return(<>
  
  </>)
}
    /*const canvasRef = useRef<HTMLCanvasElement>(null)

    useEffect(() => {
      if (!canvasRef.current) return
      const canvas = canvasRef.current
      const ctx = canvas.getContext('2d')
      if (!ctx) return
  
      const canvasDimensions : Vec2 = {x:canvas.width,y:canvas.height}
      const targetRatio = 1.6
      const canvasSize : CanvasSize = {
        ratio:targetRatio,
        size: {x:canvasDimensions.y * targetRatio,y:canvasDimensions.y}, 
      }

      const session :Session = {playersCount:1,sessionState:"Waiting"}
      const userData : UserData = {playerId:0,cards : []}
      const playerData : UserData[] = []
    }, [])
    window.requestAnimationFrame((t) => console.log(t))
    
    return (<canvas ref = {canvasRef} style = {{
        width: "90vw",
        height: "90vh",
        marginBottom: "3vh",
        marginTop: "3vh",
        marginRight: "3vw",
        marginLeft: "3vw",

    }}/>)
}

type Vec2 = Readonly<{x:number,y:number}>
type CanvasSize = {size:Vec2, ratio: number}
type InputEvent = {type:"move",direction:Vec2} | {type:"click",position:Vec2}
type RenderEffect = {type: "drawRect", pos:Vec2, color:string} | {type:"clearScreen"}

type Card = {cardType:number,cardDescription:string}
type PlayerData = {playerId:number,revealedCards : Card[], cardsLeft : number}
type UserData = {playerId:number, cards : Card[]}
type SessionState = "ActiveGame" | "Waiting"
type Session = {playersCount:number,sessionState : SessionState}



function UpdateGame(session:Session,player:PlayerData,userData:UserData[],input:InputEvent):[Session,ReadonlyArray<RenderEffect>] {
    function HandleInput(input:InputEvent) {
        console.log(input)
    }
    HandleInput(input)
    const effects : RenderEffect[] = []
    return [session,effects]
}

*/