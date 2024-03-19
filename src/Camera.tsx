import React from 'react'
import {
  Camera as VisionCamera,
  useFrameProcessor
} from 'react-native-vision-camera'
import { useSharedValue } from 'react-native-worklets-core'
import { detectFaces } from './FaceDetector'

// types
import type {
  DependencyList,
  ForwardedRef
} from 'react'
import type {
  CameraProps,
  Frame,
  FrameInternal
} from 'react-native-vision-camera'
import type {
  CallbackType,
  FaceDetectionOptions
} from './FaceDetector'

type WorkletType = (
  frame: FrameInternal
) => Promise<void>

type ComponentType = {
  faceDetectionOptions?: FaceDetectionOptions
  faceDetectionCallback: CallbackType
} & CameraProps

/**
 * Create a Worklet function that persists between re-renders.
 * The returned function can be called from both a Worklet context and the JS context, but will execute on a Worklet context.
 *
 * @param {function} func The Worklet. Must be marked with the `'worklet'` directive.
 * @param {DependencyList} dependencyList The React dependencies of this Worklet.
 * @returns {WorkletType} A memoized Worklet
 */
function useWorklet(
  func: ( frame: FrameInternal ) => void,
  dependencyList: DependencyList
): WorkletType {
  const worklet = React.useMemo( () => {
    const context: any = 'VisionCamera.async'
    return Worklets.createRunInContextFn( func, context )
  }, dependencyList )

  return worklet
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
  ...props
}: ComponentType,
  ref: ForwardedRef<VisionCamera>
) => {
  /** 
   * Is there an async task already running?
   */
  const isAsyncContextBusy = useSharedValue( false )
  /** 
   * Throws logs/errors back on js thread
   */
  const logOnJs = Worklets.createRunInJsFn( (
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
   * Async context that will handle face detection
   */
  const runOnAsyncContext = useWorklet( (
    frame: FrameInternal
  ) => {
    'worklet'
    try {
      detectFaces(
        frame,
        faceDetectionCallback,
        faceDetectionOptions
      )
    } catch ( error: any ) {
      logOnJs( 'Execution error:', error )
    } finally {
      frame.decrementRefCount()
      isAsyncContextBusy.value = false
    }
  }, [
    faceDetectionOptions,
    faceDetectionCallback
  ] )
  /**
   * Detect faces on frame on an async context without blocking camera preview
   * 
   * @param {Frame} frame Current frame
   */
  function runAsync( frame: Frame ) {
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
 * Camera frame processor
 */
  const cameraFrameProcessor = useFrameProcessor( ( frame ) => {
    'worklet'
    runAsync( frame )
  }, [ runOnAsyncContext ] )

  return <VisionCamera
    { ...props }
    ref={ ref }
    frameProcessor={ cameraFrameProcessor }
    pixelFormat='yuv'
  />
} )
