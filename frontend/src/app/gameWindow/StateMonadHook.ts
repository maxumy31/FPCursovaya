import * as S from "fp-ts/lib/State"
import { useReducer, useState } from "react"

export const useStateMonad = <S,>(initialState: S) => {
    const [state, setState] = useState<S>(initialState);
    
    const runState = async <A>(stateAction: S.State<S, A>): Promise<A> => {
      const [result, newState] = stateAction(state);
      
      setState(newState);
      
      if (result instanceof Promise) {
        return result.then(async (asyncResult) => {
          if (typeof asyncResult === 'function') {
            const [finalResult, finalState] = asyncResult();
            setState(finalState);
            return finalResult;
          }
          return asyncResult;
        });
      }
      
      return result;
    };
  
    return [state, runState] as const;
  };