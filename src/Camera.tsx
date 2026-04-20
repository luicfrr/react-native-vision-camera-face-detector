import React, {
  useEffect
} from 'react'
import {
  Camera as VisionCamera,
  useAsyncRunner,
  useFrameOutput
} from 'react-native-vision-camera'
import {
  Worklets,
  useSharedValue
} from 'react-native-worklets-core'
import { scheduleOnRN } from 'react-native-worklets'
import { SkiaCamera } from 'react-native-vision-camera-skia'
import { useFaceDetector } from './FaceDetector'

// types
import type {
  ComponentProps,
  DependencyList,
  RefObject
} from 'react'
import type {
  CameraProps,
  CameraRef,
  Frame
} from 'react-native-vision-camera'
import type { SkiaCameraProps } from 'react-native-vision-camera-skia'
import type {
  Face,
  FrameFaceDetectionOptions
} from './FaceDetector'

type UseWorkletType = (
  frame: Frame
) => Promise<void>

type UseRunInJSType = (
  faces: Face[],
  frame: Frame
) => Promise<void | Promise<void>>

type CallbackType = (
  faces: Face[],
  frame: Frame
) => void | Promise<void>

type OnFrameType = ComponentProps<typeof SkiaCamera>[ 'onFrame' ]
type ComponentType = ( ( {
  ref: RefObject<CameraRef | null>
  faceDetectionOptions?: FrameFaceDetectionOptions
  faceDetectionCallback: CallbackType
  skiaActions?: undefined
} & CameraProps ) | ( {
  ref: RefObject<CameraRef | null>
  faceDetectionOptions?: FrameFaceDetectionOptions
  faceDetectionCallback: CallbackType
  skiaActions: (
    faces: Face[],
    frame: Parameters<NonNullable<OnFrameType>>[ 0 ],
    render: Parameters<NonNullable<OnFrameType>>[ 1 ]
  ) => void | Promise<void>
} ) & SkiaCameraProps )

/**
 * Create a Worklet function that persists between re-renders.
 * The returned function can be called from both a Worklet context and the JS context, but will execute on a Worklet context.
 *
 * @param {function} func The Worklet. Must be marked with the `'worklet'` directive.
 * @param {DependencyList} dependencyList The React dependencies of this Worklet.
 * @returns {UseWorkletType} A memoized Worklet
 */
function useWorklet(
  func: ( frame: Frame ) => void,
  dependencyList: DependencyList
): UseWorkletType {
  const worklet = React.useMemo( () => {
    const context = Worklets.createContext( 'FaceDetectorContext' )
    return context.createRunAsync( func )
  }, dependencyList )

  return worklet
}

/**
 * Create a Worklet function that runs the giver function on JS context.
 * The returned function can be called from a Worklet to hop back to the JS thread.
 * 
 * @param {function} func The Worklet. Must be marked with the `'worklet'` directive.
 * @param {DependencyList} dependencyList The React dependencies of this Worklet.
 * @returns {UseRunInJSType} a memoized Worklet
 */
function useRunInJS(
  func: CallbackType,
  dependencyList: DependencyList
): UseRunInJSType {
  return React.useMemo( () => (
    Worklets.createRunOnJS( func )
  ), dependencyList )
}

/**
 * Vision camera wrapper
 * 
 * @param {ComponentType} props Camera + face detection props 
 * @returns 
 */
export function Camera( {
  ref,
  faceDetectionOptions,
  faceDetectionCallback,
  skiaActions,
  ...props
}: ComponentType ) {
  const asyncRunner = useAsyncRunner()
  const faces = useSharedValue<string>( '[]' )
  const {
    detectFaces,
    stopListeners
  } = useFaceDetector( faceDetectionOptions )
  const { autoMode } = faceDetectionOptions ?? {}

  useEffect( () => {
    return () => stopListeners()
  }, [] )

  /** 
   * Throws logs/errors back on js thread
   */
  const logOnJs = Worklets.createRunOnJS( (
    log: string,
    error?: Error
  ) => {
    if ( error ) {
      console.error( log, error.message ?? JSON.stringify( error ) )
    } else {
      console.log( log )
    }
  } )

  /**
   * Runs on detection callback on js thread
   */
  const runOnJs = useRunInJS( faceDetectionCallback, [
    faceDetectionCallback
  ] )

  /**
   * Async context that will handle face detection
   */
  const runOnAsyncContext = useWorklet( (
    frame: Frame
  ) => {
    'worklet'
    const finished = asyncRunner.runAsync( () => {
      'worklet'
      try {
        faces.value = JSON.stringify(
          detectFaces( frame )
        )
        // increment frame count so we can use frame on 
        // js side without frame processor getting stuck
        scheduleOnRN(
          JSON.parse(
            faces.value
          ),
          frame
        )
      } catch ( error: any ) {
        logOnJs( 'Execution error:', error )
      } finally {
        frame.dispose()
      }
    } )

    if ( !finished ) {
      frame.dispose()
    }
  }, [
    detectFaces,
    runOnJs
  ] )

  /**
   * Default frame output
   */
  const frameOutput = useFrameOutput( {
    pixelFormat: 'yuv',
    onFrame: ( frame ) => {
      'worklet'
      runOnAsyncContext( frame )
    }
  } )

  if (
    !autoMode &&
    !!skiaActions
  ) return <SkiaCamera
    { ...props as SkiaCameraProps }
    onFrame={ ( frame, render ) => {
      'worklet'
      skiaActions(
        JSON.parse( faces.value ),
        frame,
        render
      )
      frame.dispose()
    } }
  />

  return <VisionCamera
    { ...props }
    ref={ ref }
    outputs={ [ frameOutput ] }
  />
}
