import { WebSocketClient } from "@/app/Websocket";
import { GameStateMessage, PlayersWithReveableCards, ReveableCard } from "../WebsocketManipulations";
import { JSX } from "react";


export default function PlayingPageState(state : GameStateMessage,ws:WebSocketClient,) {
    console.log(state,"from component")
    const PlayerBox = (id:string,cards:ReveableCard[],key:number):JSX.Element => {
        return<div key = {key}>
            <div>ID игрока : {id}</div>
            <div>{cards.filter(c => c.isRevealed).map((card,i) => {
                return <ul key={i}>Тип карты: {card.cardType} Описание карты: {card.description}</ul>
            })}</div>
            <div>
            {cards.filter(c => !c.isRevealed).map((card,i) => {
                return <button key = {i}>Открыть карту {card.description}</button>
            })}
            </div>
        </div>
    }
    const BunkerInfo = (apokCard:ReveableCard, bunkerCards:ReveableCard[],id:number) => {
        return <div key = {id}>
            <div>Апокалипсис : {apokCard.description}</div>
            <div>Карты бункера : {bunkerCards.map(card => {return card.description + "   "})}</div>
        </div>
    }
    let toRender = <>Не то состояние</>
    console.log(state.type === "playingState")
    if(state.type === "playingState") {
        toRender = <>{BunkerInfo(state.apokalipsis,state.bunkerCards,0)}{state.players.map((pl,i) => {return PlayerBox(pl.id,pl.cards,i)})}</>
    } 

    return(
        <>
            {toRender}
        </>
    )
}