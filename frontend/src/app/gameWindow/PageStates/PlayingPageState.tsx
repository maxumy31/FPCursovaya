import { WebSocketClient } from "@/app/Websocket";
import { GameStateMessage, PlayersWithReveableCards, PrepareRevealCardMessage, ReveableCard } from "../WebsocketManipulations";
import { JSX } from "react";
import { Int } from "io-ts";


export default function PlayingPageState(state : GameStateMessage,ws:WebSocketClient, userId : string,sessionId:string) {

    function PrepareButtonClick(cardId:number): ()=>void {
        console.log("Prepare button : ",cardId)
        function ButtonClick():void {
            const msg = PrepareRevealCardMessage(userId,cardId,sessionId)
            ws.Send(JSON.stringify(msg))()
            console.log("Sending : ",msg)
        }
        return ButtonClick
    }

    const PlayerBox = (id:string,cards:ReveableCard[],key:number,drawCardButtons:boolean):JSX.Element => {
        return<div key = {key}>
            <div>
                {drawCardButtons && <>Ходит : </>}
                ID игрока : {id}</div>
            <div>{cards.filter(c => c.isRevealed).map((card,i) => {
                return <ul key={i}>Описание карты: {card.description}</ul>
            })}</div>
            <div>
            {drawCardButtons && cards.map((card,i) => {
                if(!card.isRevealed) {
                    return <button key = {i} onClick = {PrepareButtonClick(i)}>Открыть карту {card.description}</button>
                } else {<></>}
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
    if(state.type === "playingState") {
        toRender = <>
        {BunkerInfo(state.apokalipsis,state.bunkerCards,0)}
        {state.players.map((pl,i) => {
            const turn = state.turn
            return PlayerBox(pl.id,pl.cards,i,i == turn)
            })}
        </>
    } 

    return(
        <>
            {toRender}
        </>
    )
}