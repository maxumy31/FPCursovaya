import * as E from "fp-ts/Either";
import * as TE from "fp-ts/TaskEither";
import * as IO from "fp-ts/IO";
import * as TO from "fp-ts/TaskOption";
import { pipe } from "fp-ts/function";

type Resolver<T> = {
  resolve: (value : T) => void
  reject: (reason?:unknown)=>void
}

export type LazyQueue<T> = {
  readonly Add: (data: T) => IO.IO<void>;
  readonly Read: () => Promise<T>;
  readonly CancelAll: (error: Error) => IO.IO<void>;
};

export const NewLazyQueue = <T>(): IO.IO<LazyQueue<T>> => {
  return () => {
      let queue: T[] = []
      let resolvers: Resolver<T>[] = []

      return {
          Add: (item: T) => () => {
              if (resolvers.length > 0) {
                  const { resolve } = resolvers.shift()!
                  resolve(item)
              } else {
                  queue.push(item)
              }
          },
          Read: () => {
              return new Promise<T>((resolve, reject) => {
                  if (queue.length > 0) {
                      const item = queue.shift()!
                      resolve(item)
                  } else {
                      resolvers.push({ resolve, reject })
                  }
              });
          },
          CancelAll: (error: Error) => () => {
              resolvers.forEach(({ reject }) => reject(error))
              resolvers = []
              queue = []
          },
      };
  };
};
