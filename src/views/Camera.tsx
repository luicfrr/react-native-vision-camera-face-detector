import React, {
  useCallback,
  useRef
} from 'react'
import { Camera as VisionCamera } from 'react-native-vision-camera'
import { transformFacesToPreviewCoordinates } from '../utils/transformFacesToPreviewCoordinates'
import useFaceDetectorOutput from '../hooks/useFaceDetectorOutput'

// types
import type { RefObject } from 'react'
import type {
  CameraViewProps,
  CameraRef
}
  from 'react-native-vision-camera'
import type { FaceDetectorOutputOptions } from '../specs/FaceDetectorFactory.nitro'

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
 *       performanceMode={'fast'}
 *       runClassifications={true}
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
  mirrorMode,
  resizeMode,
  autoMode,
  performanceMode,
  runLandmarks,
  runContours,
  runClassifications,
  minFaceSize,
  trackingEnabled,
  outputs,
  ref,
  ...cameraProps
}: ComponentType ) {
  const cameraRef = useRef<CameraRef | null>( null )

  const setCameraRef = useCallback( (
    camera: CameraRef | null
  ) => {
    cameraRef.current = camera

    if ( ref != null ) {
      ref.current = camera
    }
  }, [ ref ] )

  const handleFacesDetected = useCallback( (
    faces: Parameters<typeof onFacesDetected>[ 0 ]
  ) => {
    if (
      !autoMode ||
      faces.length === 0
    ) {
      onFacesDetected( faces )
      return
    }

    const camera = cameraRef.current
    if ( camera?.preview == null ) return

    onFacesDetected(
      transformFacesToPreviewCoordinates(
        faces,
        camera
      )
    )
  }, [
    autoMode,
    onFacesDetected
  ] )

  const output = useFaceDetectorOutput( {
    onFacesDetected: handleFacesDetected,
    onError,
    outputResolution,
    cameraFacing,
    mirrorMode,
    autoMode,
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled,
  } )

  return (
    <VisionCamera
      { ...cameraProps }
      ref={ setCameraRef }
      mirrorMode={ mirrorMode }
      resizeMode={ resizeMode }
      outputs={ [ output, ...( outputs ?? [] ) ] }
      onError={ onError }
    />
  )
}

export default Camera
