import { WebSocketClient } from "@/app/Websocket"
import { JSX } from "react"
import { PrepareStartGameMessage } from "../WebsocketManipulations"

export default function WaitingPageState(players : string[],currentId:string,sessionId:string,ws:WebSocketClient):JSX.Element {
    const isHost = currentId == players[0]

    function StartGame():void {
        const startGameCommand = PrepareStartGameMessage(currentId,sessionId)
        const message = JSON.stringify(startGameCommand)
        ws.Send(message)()
        console.log("sent")
    }

    const startGameButton = <button onClick = {StartGame}>Начать игру</button>
    return(
        <div> 
            <div>
                Ваш ID - {currentId}. ID сессии - {sessionId}
            </div>
            <div>
                Ожидаем игры. Всего : {players.length} игроков
            </div>
            {isHost && startGameButton}
            <div>
                Подключены следующие игроки:
                {players.map((pl,id) => {
                    return <ul key = {id}>{pl}</ul>
                })}
            </div>
        </div>
    )
}