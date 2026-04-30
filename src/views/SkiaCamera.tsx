import React, {
  useEffect
} from 'react'
import { SkiaCamera as VisionSkiaCamera } from 'react-native-vision-camera-skia'
import { useAsyncRunner } from 'react-native-vision-camera'
import useFaceDetector from '../hooks/useFaceDetector'

// types
import type { SkiaCameraProps } from 'react-native-vision-camera-skia'
import type { FaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'

type ComponentType = SkiaCameraProps & FaceDetectorOptions

/**
 * Vision camera wrapper
 * 
 * @param {ComponentType} props Camera + face detection props 
 * @returns 
 */
export function SkiaCamera( {
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
    autoMode: undefined,
    windowWidth: undefined,
    windowHeight: undefined
  } )

  useEffect( () => {
    return () => faceDetector.stopListeners()
  }, [] )

  return <VisionSkiaCamera
    { ...cameraProps }
    pixelFormat='yuv'
    onFrame={ (
      frame,
      render
    ) => {
      'worklet'

      cameraProps.onFrame?.(
        frame,
        render
      )

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
    } }
  />
}

export default SkiaCamera
