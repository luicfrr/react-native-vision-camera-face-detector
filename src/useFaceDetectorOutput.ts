import {
  useMemo,
  useRef
} from 'react'
import { createFaceDetectorOutput } from './factory'

// types
import type {
  Camera,
  CameraOutput,
  CameraSession,
} from 'react-native-vision-camera'
import type { FaceDetectorOutputOptions } from './specs/FaceDetectorFactory.nitro'

/**
 * Use a Face Detector {@linkcode CameraOutput}.
 *
 * The Face Detector {@linkcode CameraOutput} can be
 * attached to a {@linkcode CameraSession} or {@linkcode Camera}
 * component.
 *
 * @example
 * Attach to a `<Camera />` component:
 * ```tsx
 * const device = ...
 * const faceDetectorOutput = useFaceDetectorOutput({
 *   performanceMode: 'fast',
 *   onFaceDetected: (faces) => {
 *     console.log(`Detected ${faces.length} faces!`)
 *   },
 *   onError: (error) => {
 *     console.error(`Failed to detect faces!`, error)
 *   }
 * })
 *
 * return (
 *   <Camera
 *     isActive={true}
 *     device={device}
 *     outputs={[faceDetectorOutput]}
 *   />
 * )
 * ```
 * @example
 * Attach to a `CameraSession`:
 * ```ts
 * const device = ...
 * const faceDetectorOutput = useFaceDetectorOutput({
 *   performanceMode: 'fast',
 *   onFaceDetected: (faces) => {
 *     console.log(`Detected ${faces.length} faces!`)
 *   },
 *   onError: (error) => {
 *     console.error(`Failed to detect faces!`, error)
 *   }
 * })
 * const camera = useCamera({
 *   isActive: true,
 *   device: device,
 *   outputs: [faceDetectorOutput]
 * })
 * ```
 */
export function useFaceDetectorOutput( {
  outputResolution = 'preview',
  onFaceDetected,
  onError,
  ...faceDetectorOptions
}: FaceDetectorOutputOptions ): CameraOutput {
  const stableOnFaceDetected = useRef( onFaceDetected )
  stableOnFaceDetected.current = onFaceDetected

  const stableOnError = useRef( onError )
  stableOnError.current = onError

  return useMemo(
    () => (
      createFaceDetectorOutput( {
        ...faceDetectorOptions,
        outputResolution: outputResolution,
        onFaceDetected( faces ) {
          stableOnFaceDetected.current( faces )
        },
        onError( error ) {
          stableOnError.current( error )
        }
      } )
    ), [
    faceDetectorOptions,
    outputResolution
  ] )
}

export default useFaceDetectorOutput
