import { useMemo } from 'react'
import { createImageFaceDetector } from './factory'

// types
import type { Face } from './specs/Face.nitro'
import type { ImageFaceDetector } from './specs/ImageFaceDetector.nitro'
import type { ImageFaceDetectorOptions } from './specs/ImageFaceDetectorFactory.nitro'

/**
 * Use a {@linkcode ImageFaceDetector}.
 *
 * A {@linkcode ImageFaceDetector} can be used to detect
 * {@linkcode Face}s in any image provided.
 *
 * @example
 * ```ts
 * const uri = 'file:///storage/emulated/0/Download/pic.jpg'
 * const ImageFaceDetector = useFaceDetector({...})
 * const faces = useImageFaceDetector.detectFaces(uri)
 * ```
 */
export function useImageFaceDetector(
  options?: ImageFaceDetectorOptions
): ImageFaceDetector {
  return useMemo(
    () => createImageFaceDetector( options ),
    [ options ]
  )
}

export default useImageFaceDetector
