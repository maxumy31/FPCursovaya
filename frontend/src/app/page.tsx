'use client'

import { useEffect, useRef, useState } from "react"
import styles from "./../../public/CssModules/AuthPage.module.css"
import * as HTTP from "@/app/HTTP"
import * as E from "fp-ts/Either";
import * as TE from "fp-ts/TaskEither";
import * as IO from "fp-ts/IO";
import * as O from "fp-ts/Option"
import { pipe } from "fp-ts/lib/function";
import Loading from "./components/Loading";
import { useRouter } from "next/navigation";
import { error } from "console";

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


const IdRespToId = (data:string): O.Option<number> => {
    const findId = (json:any) : O.Option<number> => json["data"] && 
        json["data"]["id"] && json["errors"] == null ? O.some(json["data"]["id"]) : O.none
        
    return pipe(
        data,
        findId,
    )
}

const sesIdRespToSesId = (data:string): O.Option<number> => {
    const findId = (json:any) : O.Option<number> => json["data"] && 
        json["data"]["id"] && json["errors"] == null && json["data"] != "Session not found" ? O.some(json["data"]["id"]) : O.none
        
    return pipe(
        data,
        findId,
    )
}


export default function AuthPage() {
    const [isLoading, setLoaded] = useState(true)
    const [userId,setUserId] = useState(0)
    const [errorMessage,setErrorMessage] = useState("")
    const sessionIdInputRef = useRef<HTMLInputElement>(null);
    const router = useRouter()


    const OnCreateNewSessionButtonClick = ():void => {
        createNewSession(config)().then(v => {
            setLoaded(false)
            switch(v._tag) {
                case "Left":
                    console.log("No response from server")
                    setLoaded(true)
                    break
                case "Right":
                    console.log(v.right)
                    const id = IdRespToId(v.right)
                    switch(id._tag) {
                        case "None":
                            console.log("No response from server")
                            setLoaded(true)
                            break
                        case "Some":
                            const data = {
                                "sessionId":id.value,
                                "userId":userId,
                            }
                            const dataParams = new URLSearchParams({
                                data: JSON.stringify(data)
                            })
                            router.push(`/gameWindow?${dataParams}`)
                              break
                    }
            }
        })

        
    }

    const OnEnterExistingSessionClick = ():void => {
        if(!sessionIdInputRef.current) {
            return
        }
        const sessionId = sessionIdInputRef.current.value
        checkSessionExisting(sessionId)(config)().then(v => {
            setLoaded(false)
            switch(v._tag) {
                case "Left":
                    console.log("No response from server")
                    setLoaded(true)
                    break
                case "Right":
                    console.log(v.right)
                    const id = IdRespToId(v.right)
                    switch(id._tag) {
                        case "None":
                            console.log("Session does not exist")
                            setErrorMessage("Сессия не найдена.")
                            setLoaded(false)
                            break
                        case "Some":
                            router.push("/gameWindow")
                    }
            }
        })
    }

    
    useEffect(() => {
        fetchId(config)().then( v=> {
            switch(v._tag) {
                case "Left":
                    console.log("No response from server.")
                    break
                case "Right":
                    const id = (IdRespToId(v.right))
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
        <div><input ref = {sessionIdInputRef} className={styles.center_input} type = "number" /></div>
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