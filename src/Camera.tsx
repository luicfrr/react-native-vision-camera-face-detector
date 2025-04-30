import React, {
  useRef
} from 'react'
import {
  Camera as VisionCamera,
  // runAsync,
  useFrameProcessor,
  useSkiaFrameProcessor
} from 'react-native-vision-camera'
import {
  Worklets,
  // useRunOnJS,
  useSharedValue
} from 'react-native-worklets-core'
import { useFaceDetector } from './FaceDetector'

// types
import type {
  DependencyList,
  ForwardedRef
} from 'react'
import type {
  CameraProps,
  DrawableFrame,
  Frame,
  FrameInternal
} from 'react-native-vision-camera'
import type {
  Face,
  FaceDetectionOptions
} from './FaceDetector'

type UseWorkletType = (
  frame: FrameInternal
) => Promise<void>

type UseRunInJSType = (
  faces: Face[],
  frame: Frame
) => Promise<void | Promise<void>>

type CallbackType = (
  faces: Face[],
  frame: Frame
) => void | Promise<void>

type ComponentType = {
  faceDetectionOptions?: FaceDetectionOptions
  faceDetectionCallback: CallbackType
  skiaActions?: (
    faces: Face[],
    frame: DrawableFrame
  ) => void | Promise<void>
} & CameraProps

/**
 * Create a Worklet function that persists between re-renders.
 * The returned function can be called from both a Worklet context and the JS context, but will execute on a Worklet context.
 *
 * @param {function} func The Worklet. Must be marked with the `'worklet'` directive.
 * @param {DependencyList} dependencyList The React dependencies of this Worklet.
 * @returns {UseWorkletType} A memoized Worklet
 */
function useWorklet(
  func: ( frame: FrameInternal ) => void,
  dependencyList: DependencyList
): UseWorkletType {
  const worklet = React.useMemo( () => {
    const context = Worklets.defaultContext
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
export const Camera = React.forwardRef( ( {
  faceDetectionOptions,
  faceDetectionCallback,
  skiaActions,
  ...props
}: ComponentType,
  ref: ForwardedRef<VisionCamera>
) => {
  const { detectFaces } = useFaceDetector( faceDetectionOptions )
  /** 
   * Is there an async task already running?
   */
  const isAsyncContextBusy = useSharedValue( false )
  const faces = useRef<Face[]>( [] )

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
    frame: FrameInternal
  ) => {
    'worklet'
    try {
      faces.current = detectFaces( frame )
      // increment frame count so we can use frame on 
      // js side without frame processor getting stuck
      frame.incrementRefCount()
      runOnJs(
        faces.current,
        frame
      ).finally( () => {
        'worklet'
        // finally decrement frame count so it can be dropped
        frame.decrementRefCount()
      } )
    } catch ( error: any ) {
      logOnJs( 'Execution error:', error )
    } finally {
      frame.decrementRefCount()
      isAsyncContextBusy.value = false
    }
  }, [
    detectFaces,
    runOnJs
  ] )

  /**
   * Detect faces on frame on an async context without blocking camera preview
   * 
   * @param {Frame} frame Current frame
   */
  function runAsync( frame: Frame | DrawableFrame ) {
    'worklet'
    if ( isAsyncContextBusy.value ) return
    // set async context as busy
    isAsyncContextBusy.value = true
    // cast to internal frame and increment ref count
    const internal = frame as FrameInternal
    internal.incrementRefCount()
    // detect faces in async context
    runOnAsyncContext( internal )
  }

  /**
   * Skia frame processor
   */
  const skiaFrameProcessor = useSkiaFrameProcessor( ( frame ) => {
    'worklet'
    frame.render()
    skiaActions!( faces.current, frame )
    runAsync( frame )
  }, [
    runOnAsyncContext,
    skiaActions
  ] )

  /**
   * Default frame processor
   */
  const cameraFrameProcessor = useFrameProcessor( ( frame ) => {
    'worklet'
    runAsync( frame )
  }, [ runOnAsyncContext ] )

  /**
   * Camera frame processor
   */
  const frameProcessor = ( () => {
    const { autoMode } = faceDetectionOptions ?? {}
    if (
      !autoMode &&
      !!skiaActions
    ) return skiaFrameProcessor

    return cameraFrameProcessor
  } )()

  //
  // use bellow when vision-camera's  
  // context creation issue is solved
  //
  // /**
  //  * Runs on detection callback on js thread
  //  */
  // const runOnJs = useRunOnJS( faceDetectionCallback, [
  //   faceDetectionCallback
  // ] )

  // const cameraFrameProcessor = useFrameProcessor( ( frame ) => {
  //   'worklet'
  //   runAsync( frame, () => {
  //     'worklet'
  //     runOnJs(
  //       detectFaces( frame ),
  //       frame
  //     )
  //   } )
  // }, [ runOnJs ] )

  return <VisionCamera
    { ...props }
    ref={ ref }
    frameProcessor={ frameProcessor }
    pixelFormat='yuv'
  />
} )
