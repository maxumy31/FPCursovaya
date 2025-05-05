import { tryCatchK, fromIO, taskEither, chain } from 'fp-ts/TaskEither';
import { IO, io } from 'fp-ts/IO';
import { pipe } from 'fp-ts/function';
import * as TE from 'fp-ts/lib/TaskEither';
import * as TO from 'fp-ts/lib/TaskOption';
import { left } from 'fp-ts/lib/EitherT';
import { resolve } from 'path';
import * as LQ from "@/app/LazyQueue"
import * as E from 'fp-ts/lib/Either';



export type WebSocketClient = {
    Send(message:string): TO.TaskOption<void>,
    Read(): TO.TaskOption<string>,
    Close(): void,
}



export const NewWebsocketClient = (url: string): TO.TaskOption<WebSocketClient> => {
  return TO.tryCatch(
    async () => {
      const ws = new WebSocket(url);
      
      await new Promise<void>((resolve, reject) => {
        ws.onopen = () => resolve();
        ws.onerror = (error) => reject(new Error('WebSocket connection error'));
        ws.onclose = (event) => reject(new Error(`Connection closed: ${event.reason}`));
      });

      const queue = LQ.NewLazyQueue<string>()();

      ws.onmessage = (event) => {
        queue.Add(event.data.toString())();
      };

      ws.onerror = () => {
        queue.CancelAll(new Error('WebSocket error'))();
      };

      ws.onclose = (event) => {
        queue.CancelAll(new Error(`WebSocket closed: ${event.reason}`))();
      };

      return {
        Send: (message: string) => TO.tryCatch(
          () => new Promise<void>((resolve, reject) => {
            if (ws.readyState === WebSocket.OPEN) {
              try {
                ws.send(message);
                resolve();
              } catch (err) {
                reject(err);
              }
            } else {
              reject(new Error('WebSocket is not open'));
            }
          })
        ),
        Read: () => TO.tryCatch(() => {
          return queue.Read()
        }),
        Close: () => {
          ws.close();
          queue.CancelAll(new Error('WebSocket closed by client'))();
        },
      };
    }
  );
};