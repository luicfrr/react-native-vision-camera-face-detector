import React, {
  useEffect
} from 'react'
import {
  Camera as VisionCamera,
  useAsyncRunner,
  useFrameOutput
} from 'react-native-vision-camera'
import { useSharedValue } from 'react-native-reanimated'
import useFaceDetector from '../useFaceDetector'
import useRunInJS from '../hooks/useRunInJs'
import useWorklet from '../hooks/useWorklet'

// types
import type { RefObject } from 'react'
import type {
  CameraProps,
  CameraRef,
  Frame
} from 'react-native-vision-camera'
import type { FaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'
import type { FaceDetectedCallback } from '../specs/FaceDetectedCallback'

type ComponentType = ( {
  ref: RefObject<CameraRef | null>
  faceDetectorOptions?: FaceDetectorOptions
  faceDetectorCallback: FaceDetectedCallback
} & CameraProps )

/**
 * Vision camera wrapper
 * 
 * @param {ComponentType} props Camera + face detection props 
 * @returns 
 */
export function Camera( {
  ref,
  faceDetectorOptions,
  faceDetectorCallback,
  ...props
}: ComponentType ) {
  const asyncRunner = useAsyncRunner()
  const faces = useSharedValue<string>( '[]' )
  const {
    detectFaces,
    stopListeners
  } = useFaceDetector( faceDetectorOptions )

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
  const runOnJs = useRunInJS(
    faceDetectorCallback, [
    faceDetectorCallback
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
        logOnJs( 'Face detector execution error:', error )
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

  return <VisionCamera
    { ...props }
    ref={ ref }
    outputs={ [ frameOutput ] }
  />
}
