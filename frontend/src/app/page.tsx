'use client'

import { useEffect, useRef, useState } from "react"
import styles from "./../../public/CssModules/AuthPage.module.css"
import * as HTTP from "@/app/HTTP"
import * as E from "fp-ts/Either";
import * as T from "fp-ts/Task";
import * as TE from "fp-ts/TaskEither";
import * as IO from "fp-ts/IO";
import * as O from "fp-ts/Option"
import { pipe } from "fp-ts/lib/function";
import Loading from "./components/Loading";
import { useRouter } from "next/navigation";
import { error } from "console";
import { GameStateMessage, parseGameStateMessage, PlayersWithReveableCards, ReveableCard } from "./gameWindow/WebsocketManipulations";

const config : HTTP.HTTPConfig = {
    url: "http://localhost:9090"
}

const fetchId = HTTP.FPFetch<string>({
    method:"POST",
    path:"/guest"
})


const createNewSession = HTTP.FPFetch<string>({
    method:"POST",
    path:"/session"
})


const checkSessionExisting = (n : string) => HTTP.FPFetch<string>({
    method:"GET",
    path:"/session/" + n
})


const getIdFromJSON = (data:string): O.Option<string> => {
    const findId = (json:any) : O.Option<string> => json["data"] && 
        json["data"]["id"] && json["errors"] == null ? O.some(json["data"]["id"]) : O.none
        
    return pipe(
        data,
        findId,
    )
}

const getSessionIdFromJson = (data:string): O.Option<string> => {
    const findId = (json:any) : O.Option<string> => json["data"] && 
        json["data"]["id"] && json["errors"] == null && json["data"] != "Session not found" ? O.some(json["data"]["id"]) : O.none
        
    return pipe(
        data,
        findId,
    )
}


export default function AuthPage() {
    const [isLoading, setLoaded] = useState(true)
    const [userId,setUserId] = useState("")
    const [errorMessage,setErrorMessage] = useState("")
    const sessionIdInputRef = useRef<HTMLInputElement>(null);
    const router = useRouter()


  

    const OnCreateNewSessionButtonClick = ():void => {
        const readValue = pipe(
            createNewSession(config),
            T.map(eith => {
                return pipe(
                    eith,
                    O.fromEither
                )
            }),
        )
        readValue().then(sessionIdOpt => {
            pipe(
                sessionIdOpt,
                O.chain(getSessionIdFromJson),
                O.map(sessionId => {
                    const data = {
                        "sessionId":sessionId,
                        "userId":userId,
                    }
                    const dataParams = new URLSearchParams({
                        data: JSON.stringify(data)
                    })
                    console.log("data :",data)
                    router.push(`/gameWindow?${dataParams}`)
                })
            )
        })
        
    }

    const OnEnterExistingSessionClick = ():void => {
        const a = pipe(
            sessionIdInputRef.current,
            O.fromNullable,
            O.map(input => {return input.value}),
            O.map(
                sessionId => {
                    const b = pipe(
                        checkSessionExisting(sessionId)(config),
                        TE.map(
                            (resp:string) => {
                                const idOpt = getIdFromJSON(resp)
                                pipe(
                                    idOpt,
                                    O.map(id => {
                                        const data = {
                                            "sessionId":sessionId,
                                            "userId":userId,
                                        }
                                        const dataParams = new URLSearchParams({
                                            data: JSON.stringify(data)
                                        })
                                        console.log("Loggint with :",data)
                                        router.push(`/gameWindow?${dataParams}`)
                                    })
                                    
                                )
                            }
                        )
                    )
                    b()
                }
            )
        )
    }

    
    useEffect(() => {
        fetchId(config)().then( v=> {
            switch(v._tag) {
                case "Left":
                    console.log("No response from server.")
                    break
                case "Right":
                    const id = (getIdFromJSON(v.right))
                    switch(id._tag) {
                        case "None":
                            console.log("Wrong response from server.")
                            break
                        case "Some":
                            setLoaded(false)
                            setUserId(id.value)
                    }

            }
        })
        

    },[])

    const page = <div className={styles.wrap}>
    <div></div>
    <div className={styles.center_block}>
        <div>Ваш ID = {userId}</div>
        <div>Создать новую игру или присоединиться к уже созданной?</div>
        <div><input ref = {sessionIdInputRef} className={styles.center_input}/></div>
        {errorMessage}
        <div><button onClick = {OnEnterExistingSessionClick} className={styles.center_button}>Зайти</button></div>
        <div><button onClick = {OnCreateNewSessionButtonClick} className={styles.center_button}>Создать новую</button></div>
        </div>
    <div></div>
</div>

    return (<>
        {isLoading && <Loading/>}
        {page}
    </>
    )
}