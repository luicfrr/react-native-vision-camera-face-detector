import React from 'react'
import { Camera as VisionCamera } from 'react-native-vision-camera'

// types
import type { RefObject } from 'react'
import type {
  CameraViewProps,
  CameraRef
} from 'react-native-vision-camera'
import type { FaceDetectorOutputOptions } from '../specs/FaceDetectorFactory.nitro'
import useFaceDetectorOutput from '../hooks/useFaceDetectorOutput'

interface ComponentType
  extends Omit<CameraViewProps, 'onError'>,
  FaceDetectorOutputOptions {
  ref?: RefObject<CameraRef | null>
}

/**
 * A view that detects {@linkcode Face}s in a Camera
 * using the default front {@linkcode CameraDevice}.
 *
 *
 * @example
 * ```tsx
 * function App() {
 *   const isFocused = useIsFocused()
 *   const appState = useAppState()
 *   const isActive = isFocused && appState === 'active'
 *   return (
 *     <Camera
 *       isActive={isActive}
 *       barcodeFormats={['all']}
 *       onFacesDetected={(faces) => {
 *         console.log(`Detected ${faces.length} faces!`)
 *       }}
 *       onError={(error) => {
 *         console.error(`Error detecting faces:`, error)
 *       }}
 *     />
 *   )
 * }
 * ```
 */
export function Camera( {
  onFacesDetected,
  onError,
  outputResolution,
  cameraFacing,
  autoMode,
  windowWidth,
  windowHeight,
  performanceMode,
  runLandmarks,
  runContours,
  runClassifications,
  minFaceSize,
  trackingEnabled,
  outputs,
  ...cameraProps
}: ComponentType ) {
  const output = useFaceDetectorOutput( {
    onFacesDetected,
    onError,
    outputResolution,
    cameraFacing,
    autoMode,
    windowWidth,
    windowHeight,
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled,
  } )

  return <VisionCamera
    { ...cameraProps }
    outputs={ [
      output,
      ...( outputs ?? [] )
    ] }
    onError={ onError }
  />
}

export default Camera
