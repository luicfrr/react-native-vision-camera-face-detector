import { useMemo } from 'react'
import { scheduleOnRN } from 'react-native-worklets'

// types
import type { DependencyList } from 'react'
import type { Frame } from 'react-native-vision-camera'
import type { Face } from '../specs/Face.nitro'

export type UseRunInJSType = (
  faces: Face[],
  frame: Frame
) => Promise<void | Promise<void>>

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
): ( ...args: TArgs ) => void {
  return useMemo( () => (
    ...args: TArgs
  ) => {
    scheduleOnRN( func, ...args )
  }, dependencyList )
}

export default useRunInJS
