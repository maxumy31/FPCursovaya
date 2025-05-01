import * as E from "fp-ts/Either";
import * as TE from "fp-ts/TaskEither";
import * as IO from "fp-ts/IO";
import * as R from "fp-ts/lib/Reader";
import { pipe } from "fp-ts/lib/function";
import { reader } from "fp-ts";

export type HTTPError = {
    message: string
}

export type HTTPConfig = {
    url:string
}

export type RequestData = {
    method: "GET" | "POST" | "DELETE" | "UPDATE" | "PUT" | "PATCH"
    path: string
    body? :string
}

export type HttpResponse<T> = R.Reader<HTTPConfig, TE.TaskEither<HTTPError,T>>

export const FPFetch = <T>(config: RequestData): R.Reader<HTTPConfig, TE.TaskEither<HTTPError, T>> => 
    (env : HTTPConfig) => {
      const url = env.url + config.path
      const headers = { 'Content-type': 'application/json' }
  
      return TE.tryCatch(
        async () => {
          const response = await fetch(url, {
            method: config.method,
            headers,
            body: config.body ? JSON.stringify(config.body) : undefined
          })
          
          if (!response.ok) {
            throw new Error(`HTTP Error ${response.status}`)
          }
          
          return response.json() as T
        },
        (error): HTTPError => ({
          message: error instanceof Error ? error.message : 'Unknown error'
        })
      )
    }