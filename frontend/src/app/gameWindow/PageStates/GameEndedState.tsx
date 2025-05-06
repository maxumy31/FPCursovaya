import { JSX } from "react"
import { GameStateMessage, ReveableCard } from "../WebsocketManipulations"
import { WebSocketClient } from "@/app/Websocket"

export default function GameEndedState(state : GameStateMessage,ws:WebSocketClient, userId : string,sessionId:string) {

    const PlayerBox = (id:string,cards:ReveableCard[],key:number):JSX.Element => {
            return<div key = {key}>
                <div>
                    ID игрока : {id}</div>
                <div>{cards.filter(c => c.isRevealed).map((card,i) => {
                    return <ul key={i}>Описание карты: {card.description}</ul>
                })}</div>
            </div>
        }

    const BunkerInfo = (apokCard:ReveableCard, bunkerCards:ReveableCard[], threatCards : ReveableCard[]) => {
        return <div>
            <div>Апокалипсис : {apokCard.description}</div>
            <div>Карты бункера : {bunkerCards.map(card => {return card.description + "   "})}</div>
            <div>Карты угроз : {threatCards.map(card => {return card.description + "   "})}</div>
        </div>
    }

    let toRender = <>Не то состояние</>
    if(state.type === "gameEnded") {
        toRender = <>
        {BunkerInfo(state.apokalipsis,state.bunkerCards,state.threats)}
        {state.players.map((pl,i) => {
            return PlayerBox(pl.id,pl.cards,i)
            })}
        </>
    } 

    return (
        <>
        {toRender}
        </>
    )
}