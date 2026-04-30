import React, {
  useEffect
} from 'react'
import {
  Camera as VisionCamera,
  useAsyncRunner,
  useFrameOutput
} from 'react-native-vision-camera'
import useFaceDetector from '../hooks/useFaceDetector'

// types
import type { RefObject } from 'react'
import type {
  CameraViewProps,
  CameraRef
} from 'react-native-vision-camera'
import type { FaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'

interface ComponentType
  extends CameraViewProps, FaceDetectorOptions {
  ref?: RefObject<CameraRef | null>
}

/**
 * Vision camera wrapper
 * 
 * @param {ComponentType} props Camera + face detection props 
 * @returns 
 */
export function Camera( {
  onFacesDetected,
  onFacesDetectedError,
  performanceMode,
  runLandmarks,
  runContours,
  runClassifications,
  minFaceSize,
  trackingEnabled,
  cameraFacing,
  autoMode,
  windowWidth,
  windowHeight,
  ...cameraProps
}: ComponentType ) {
  const asyncRunner = useAsyncRunner()
  const faceDetector = useFaceDetector( {
    onFacesDetected,
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled,
    autoMode,
    windowWidth,
    windowHeight,
    cameraFacing
  } )

  useEffect( () => {
    return () => faceDetector.stopListeners()
  }, [] )

  /**
   * Default frame output
   */
  const frameOutput = useFrameOutput( {
    pixelFormat: 'yuv',
    onFrame: ( frame ) => {
      'worklet'

      const finished = asyncRunner.runAsync( () => {
        'worklet'

        try {
          faceDetector.detectFaces( frame )
        } catch ( error: any ) {
          console.error(
            'Face detector execution error:',
            error.message ?? JSON.stringify( error )
          )
          onFacesDetectedError?.( error )
        } finally {
          frame.dispose()
        }
      } )

      if ( !finished ) {
        frame.dispose()
      }
    }
  } )

  return <VisionCamera
    { ...cameraProps }
    outputs={ [ frameOutput ] }
  />
}

export default Camera
