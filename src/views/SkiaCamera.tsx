import React, {
  useEffect
} from 'react'
import { SkiaCamera as VisionSkiaCamera } from 'react-native-vision-camera-skia'
import { useAsyncRunner } from 'react-native-vision-camera'
import { useSharedValue } from 'react-native-reanimated'
import useFaceDetector from '../useFaceDetector'
import useRunInJS from '../hooks/useRunInJs'
import useWorklet from '../hooks/useWorklet'

// types
import type {
  ComponentProps,
  RefObject
} from 'react'
import type {
  CameraRef,
  Frame
} from 'react-native-vision-camera'
import type { SkiaCameraProps } from 'react-native-vision-camera-skia'
import type { Face } from '../specs/Face.nitro'
import type { FrameFaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'
import type { FaceDetectorCallback } from '../specs/FaceDetectorCallback'

type OnFrameType = ComponentProps<typeof SkiaCamera>[ 'onFrame' ]
type ComponentType = ( {
  ref: RefObject<CameraRef | null>
  faceDetectionOptions?: FrameFaceDetectorOptions
  faceDetectionCallback: FaceDetectorCallback
  skiaActions?: (
    faces: Face[],
    frame: Parameters<NonNullable<OnFrameType>>[ 0 ],
    render: Parameters<NonNullable<OnFrameType>>[ 1 ]
  ) => void | Promise<void>
} ) & SkiaCameraProps

/**
 * Vision camera wrapper
 * 
 * @param {ComponentType} props Camera + face detection props 
 * @returns 
 */
export function SkiaCamera( {
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

  useEffect( () => {
    return () => stopListeners()
  }, [] )

  /** 
     * Throws logs/errors back on js thread
     */
  const logOnJs = useRunInJS( (
    log: string,
    error?: Error
  ) => {
    if ( error ) {
      console.error( log, error.message ?? JSON.stringify( error ) )
    } else {
      console.log( log )
    }
  }, [] )

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

        runOnJs(
          JSON.parse( faces.value ),
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

  return <VisionSkiaCamera
    { ...props as SkiaCameraProps }
    pixelFormat='yuv'
    onFrame={ ( frame, render ) => {
      'worklet'

      skiaActions?.(
        JSON.parse( faces.value ),
        frame,
        render
      )

      runOnAsyncContext( frame )
      frame.dispose()
    } }
  />
}
