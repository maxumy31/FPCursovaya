import * as E from "fp-ts/Either";
import * as TE from "fp-ts/TaskEither";
import * as IO from "fp-ts/IO";
import { pipe } from "fp-ts/function";


export type LazyQueue<T,E> = {
    readonly Add:(data:T) => IO.IO<void>
    readonly Read:TE.TaskEither<E,T>
}

export type LazyQueueError = "Empty" | "Undefined"

export const NewLazyQueue = <T, E = LazyQueueError>(): IO.IO<LazyQueue<T, E>> => {
    return () => {
      let queue: T[] = [];
  
      return {
        Add: (item: T) => () => {
          queue = [...queue, item];
        },
  
        Read: pipe(
          IO.of(queue),
          IO.chain((data) => {
            if (data.length === 0) {
              return IO.of(E.left<E, T>("Empty" as E));
            }
            const [head, ...tail] = data;
            queue = tail;
            return IO.of(E.right<E, T>(head));
          }),
          TE.fromIOEither
        ),

      };
    };
  };