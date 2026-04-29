import React, {
  useEffect,
  useMemo
} from 'react'
import {
  Camera as VisionCamera,
  useAsyncRunner,
  useFrameOutput
} from 'react-native-vision-camera'
import { createSynchronizable } from 'react-native-worklets'
import useFaceDetector from '../hooks/useFaceDetector'
import useRunInJS from '../hooks/useRunInJs'

// types
import type { RefObject } from 'react'
import type {
  CameraViewProps,
  CameraRef,
  Frame
} from 'react-native-vision-camera'
import type { Face } from '../specs/Face.nitro'
import type { FaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'
import type { FaceDetectedCallback } from '../specs/FaceDetectedCallback'

type ComponentType = {
  ref?: RefObject<CameraRef | null>
  faceDetectorOptions?: FaceDetectorOptions
  faceDetectorCallback: FaceDetectedCallback
} & CameraViewProps

/**
 * Vision camera wrapper
 * 
 * @param {ComponentType} props Camera + face detection props 
 * @returns 
 */
export function Camera( {
  faceDetectorOptions,
  faceDetectorCallback,
  ...props
}: ComponentType ) {
  const asyncRunner = useAsyncRunner()
  const faces = createSynchronizable<Face[]>( [] )
  const faceDetector = useFaceDetector( faceDetectorOptions )

  useEffect( () => {
    return () => faceDetector.stopListeners()
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
  const detectFacesAsync = useMemo( () => (
    frame: Frame
  ) => {
    'worklet'

    const finished = asyncRunner.runAsync( () => {
      'worklet'

      try {
        faces.setBlocking(
          faceDetector.detectFaces( frame )
        )
      } catch ( error: any ) {
        console.error(
          'Face detector execution error:',
          error.message ?? JSON.stringify( error )
        )
      } finally {
        frame.dispose()
      }
    } )

    if ( !finished ) {
      frame.dispose()
    }

    runOnJs( faces.getDirty() )
  }, [
    asyncRunner,
    faceDetector
  ] )

  /**
   * Default frame output
   */
  const frameOutput = useFrameOutput( {
    pixelFormat: 'yuv',
    onFrame: ( frame ) => {
      'worklet'
      detectFacesAsync( frame )
    }
  } )

  return <VisionCamera
    { ...props }
    outputs={ [ frameOutput ] }
  />
}

export default Camera
