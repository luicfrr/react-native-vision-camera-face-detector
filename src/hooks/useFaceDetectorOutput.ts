import {
  useMemo,
  useRef
} from 'react'
import { createFaceDetectorOutput } from '../factory'

// types
import type {
  CameraOutput,
  CameraSession,
  Camera
} from 'react-native-vision-camera'
import type { FaceDetectorOutputOptions } from '../specs/FaceDetectorFactory.nitro'

/**
 * Use a Barcode Scanner {@linkcode CameraOutput}.
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
 *   onFacesDetected(faces) {
 *     console.log(`Detected ${faces.length} faces!`)
 *   },
 *   onError(error) {
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
 *   onFacesDetected(faces, frame) {
 *     console.log(`Detected ${faces.length} faces!`)
 *   },
 *   onError(error) {
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
  onFacesDetected,
  onError,
  outputResolution = 'preview',
  ...options
}: FaceDetectorOutputOptions ): CameraOutput {
  const stableOnFacesDetected = useRef( onFacesDetected )
  stableOnFacesDetected.current = onFacesDetected

  const stableOnError = useRef( onError )
  stableOnError.current = onError

  // `options` is a rest object, so it is a new object on every render.
  // All detector options are primitives, so memoize on their serialized value instead.
  const optionsKey = JSON.stringify( {
    ...options,
    outputResolution
  } )

  return useMemo(
    () => createFaceDetectorOutput( {
      onFacesDetected( faces ) {
        stableOnFacesDetected.current( faces )
      },
      onError( error ) {
        stableOnError.current( error )
      },
      ...JSON.parse( optionsKey ) as Omit<
        FaceDetectorOutputOptions,
        'onFacesDetected' | 'onError'
      >
    } ),
    [ optionsKey ]
  )
}

export default useFaceDetectorOutput
