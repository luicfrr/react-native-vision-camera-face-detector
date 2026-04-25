import { useMemo } from 'react'
import { createFaceDetector } from '../factory'

// types
import type { Frame } from 'react-native-vision-camera'
import type { Face } from '../specs/Face.nitro'
import type { FaceDetector } from '../specs/FaceDetector.nitro'
import type { FaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'

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
export function useFaceDetector(
  options?: FaceDetectorOptions
): FaceDetector {
  return useMemo(
    () => createFaceDetector( options ),
    [ options ]
  )
}

export default useFaceDetector
