import React from 'react'
import {
  Camera as VisionCamera,
  runAsync,
  useFrameProcessor
} from 'react-native-vision-camera'
import { Worklets } from 'react-native-worklets-core'
import { useFaceDetector } from './FaceDetector'

// types
import type { ForwardedRef } from 'react'
import type {
  CameraProps,
  Frame,
  FrameInternal
} from 'react-native-vision-camera'
import type {
  Face,
  FaceDetectionOptions
} from './FaceDetector'

type CallbackType = (
  faces: Face[],
  frame: Frame
) => void | Promise<void>

type ComponentType = {
  faceDetectionOptions?: FaceDetectionOptions
  faceDetectionCallback: CallbackType
} & CameraProps

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
  const { detectFaces } = useFaceDetector( faceDetectionOptions )

  /**
   * Runs on detection callback on js thread
   */
  const runOnJs = Worklets.createRunOnJS( faceDetectionCallback )

  /**
   * Camera frame processor
   */
  const cameraFrameProcessor = useFrameProcessor( ( frame ) => {
    'worklet'
    runAsync( frame, () => {
      'worklet'
      const internal = frame as FrameInternal
      const faces = detectFaces( frame )

      // increment frame count so we can use frame on 
      // js side without frame processor getting stuck
      internal.incrementRefCount()
      runOnJs(
        faces,
        frame
      ).finally( () => {
        // finally decrement frame count so it can be dropped
        internal.decrementRefCount()
      } )
    } )
  }, [ runOnJs ] )

  return <VisionCamera
    { ...props }
    ref={ ref }
    frameProcessor={ cameraFrameProcessor }
    pixelFormat='yuv'
  />
} )
