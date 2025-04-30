'use client'
import { readonlyArray } from 'fp-ts';
import { State } from 'fp-ts/State';
import { useRef, useEffect } from 'react'
import { pipe } from 'fp-ts/function';

export default function GameWindows(){
    const canvasRef = useRef<HTMLCanvasElement>(null)

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

