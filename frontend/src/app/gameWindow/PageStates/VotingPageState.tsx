import { WebSocketClient } from "@/app/Websocket";
import { GameStateMessage, PlayersWithReveableCards, PrepareRevealCardMessage, PrepareVotingMessage, ReveableCard } from "../WebsocketManipulations";
import { JSX } from "react";
import { Int } from "io-ts";


export default function VotingPageState(state : GameStateMessage,ws:WebSocketClient, userId : string,sessionId:string) {

    const PlayerBox = (id:string,cards:ReveableCard[],key:number, isYou : boolean,alreadyVoted : boolean):JSX.Element => {
        function VoteAgainst(uId:string,tId:string,sId:string) {
            function Vote() {
                const msg = PrepareVotingMessage(uId,sId,tId)
                ws.Send(JSON.stringify(msg))()
                console.log("Sending : ",msg)
            }
            return Vote
        }
        
        const voteButton = <button onClick={VoteAgainst(userId,id,sessionId)}>Проголосовать против</button>
        const canVote = (!isYou) && alreadyVoted
        return<div key = {key}>
            <div>
                ID игрока : {id} {canVote && voteButton}</div>
            <div>{cards.filter(c => c.isRevealed).map((card,i) => {
                return <ul key={i}>Описание карты: {card.description}</ul>
            })}</div>
        </div>
    }

    const BunkerInfo = (apokCard:ReveableCard, bunkerCards:ReveableCard[],id:number) => {
        return <div key = {id}>
            <div>Апокалипсис : {apokCard.description}</div>
            <div>Карты бункера : {bunkerCards.map(card => {return card.description + "   "})}</div>
        </div>
    }

    let toRender = <>Не то состояние</>
    if(state.type === "votingsState") {
        toRender = <>
        {BunkerInfo(state.apokalipsis,state.bunkerCards,0)}
        {state.players.map((pl,i) => {
            const turn = state.round
            const alreadyVoted = state.players.find(p => p.id === userId)?.voted == null ? true : false
            console.log(alreadyVoted, userId == pl.id)
            return PlayerBox(pl.id,pl.cards,i,userId == pl.id, alreadyVoted)
            })}
        </>
    } 

    return(
        <>
            {toRender}
        </>
    )
}