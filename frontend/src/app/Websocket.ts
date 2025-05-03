import { tryCatchK, fromIO, taskEither, chain } from 'fp-ts/TaskEither';
import { IO, io } from 'fp-ts/IO';
import { pipe } from 'fp-ts/function';
import * as TE from 'fp-ts/lib/TaskEither';
import { left } from 'fp-ts/lib/EitherT';
import { resolve } from 'path';
import * as LQ from "@/app/LazyQueue"
import * as E from 'fp-ts/lib/Either';

type WebSocketError = string;
type WebSocketClientError = LQ.LazyQueueError | string

export type WebSocketClient = {
    Send(message:string): TE.TaskEither<WebSocketError,void>,
    Read(): TE.TaskEither<string,string>,
    Close(): void,
}


export const NewWebsocketClient = (url: string): TE.TaskEither<WebSocketClientError, WebSocketClient> => {
    return TE.tryCatch(
      () =>
        new Promise<WebSocketClient>((resolve, reject) => {
          const wsConnection = NewWebsocketConnection(url);
          const queue = LQ.NewLazyQueue<string>()();
  
          wsConnection().then(either => 
            pipe(
              either,
              E.match(
                (error) => reject(error),
                (ws) => {
                  ws.onmessage = (event) => {
                      console.log("message recieved")
                      console.log("msg = ",event.data.toString())
                      queue.Add(event.data.toString())(); 
                  }
                  resolve({
                    Send: (message: string) => TE.tryCatch(
                        () => 
                          new Promise<void>((resolve, reject) => {
                            try {
                              ws.send(message);
                              resolve();
                            } catch (err) {
                              reject(err);
                            }
                          }),
                        (error) => `Send error: ${error}`
                    ),
                    Read: () => queue.Read,  
                    Close: () => {ws.close();}
                  });
                }
              )
            )
          );
        }),
      (error) => 
        `Connection failed: ${error instanceof Error ? error.message : String(error)}`
    );
  };

const NewWebsocketConnection = (url: string): TE.TaskEither<WebSocketError, WebSocket> =>
    TE.tryCatch(
      () =>
        new Promise<WebSocket>((resolve, reject) => {
          const ws = new WebSocket(url);
          ws.onopen = () => resolve(ws)
          ws.onerror = (err) => reject(err)
        }),
      (error) => `Connection failed: ${error instanceof Error ? error.message : String(error)}`
    );


const SendMessage = (ws:WebSocket, message:string):TE.TaskEither<WebSocketError,void> => 
    TE.tryCatch(
        () => new Promise<void>((resolve,reject) => {
            ws.send(message)
        }),
        (error) => `Send error ${error}`
    )