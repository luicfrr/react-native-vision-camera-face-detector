import { useMemo } from 'react'
import {
  createWorkletRuntime,
  runOnRuntimeAsync
} from 'react-native-worklets'

// types
import type { DependencyList } from 'react'
import type { Frame } from 'react-native-vision-camera'

export type UseWorkletType = (
  frame: Frame
) => Promise<void>

/**
 * Create a Worklet function that persists between re-renders.
 * The returned function can be called from both a Worklet context and the JS context, but will execute on a Worklet context.
 *
 * @param {function} func The Worklet. Must be marked with the `'worklet'` directive.
 * @param {DependencyList} dependencyList The React dependencies of this Worklet.
 * @returns {UseWorkletType} A memoized Worklet
 */
export function useWorklet(
  func: ( frame: Frame ) => void,
  dependencyList: DependencyList
): UseWorkletType {
  return useMemo( () => {
    const runtime = createWorkletRuntime( {
      name: 'FaceDetectorContext'
    } )

    return ( frame: Frame ) => runOnRuntimeAsync(
      runtime,
      func,
      frame
    )
  }, dependencyList )
}

export default useWorklet
