import {
  useMemo,
  useRef
} from 'react'
import { createFaceDetectorOutput } from '../factory'

// types
import type {
  CameraOutput,
  Frame
} from 'react-native-vision-camera'
import type { Face } from '../specs/Face.nitro'
import type { FaceDetector } from '../specs/FaceDetector.nitro'
import type { FaceDetectorOutputOptions } from '../specs/FaceDetectorFactory.nitro'

/**
 * Use a {@linkcode FaceDetector}.
 *
 * A {@linkcode FaceDetector} can be used to detect
 * {@linkcode Face}s in a {@linkcode Frame} in a Frame
 * Processor.
 *
 * @example
 * ```ts
 * const FaceDetector = useFaceDetector({...})
 * const frameOutput = useFrameOutput({
 *   onFrame(frame) {
 *     'worklet'
 *     const faces = FaceDetector.detectFaces(frame)
 *     console.log(`Detected ${faces.length} faces!`)
 *     frame.dispose()
 *   }
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

  return useMemo(
    () => createFaceDetectorOutput( {
      onFacesDetected( faces ) {
        stableOnFacesDetected.current( faces )
      },
      onError( error ) {
        stableOnError.current( error )
      },
      outputResolution: outputResolution,
      ...options
    } ),
    [ options ]
  )
}

export default useFaceDetectorOutput
