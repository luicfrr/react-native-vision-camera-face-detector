import { useMemo } from 'react'
import { scheduleOnRN } from 'react-native-worklets'

// types
import type { DependencyList } from 'react'

export type UseRunInJSType<TArgs extends unknown[]> = (
  ...args: TArgs
) => void

/**
 * Create a Worklet function that runs the giver function on JS context.
 * The returned function can be called from a Worklet to hop back to the JS thread.
 * 
 * @param {function} func The Worklet. Must be marked with the `'worklet'` directive.
 * @param {DependencyList} dependencyList The React dependencies of this Worklet.
 * @returns {UseRunInJSType} a memoized Worklet
 */
export function useRunInJS<TArgs extends unknown[]>(
  func: ( ...args: TArgs ) => void | Promise<void>,
  dependencyList: DependencyList
): UseRunInJSType<TArgs> {
  return useMemo( () => (
    ...args: TArgs
  ) => {
    'worklet'
    scheduleOnRN( func, ...args )
  }, dependencyList )
}

export default useRunInJS
