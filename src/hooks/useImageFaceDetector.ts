import { useMemo } from 'react'
import { Image } from 'react-native'
import { createImageFaceDetector } from '../factory'

// types
import type {
  ImageFaceDetector,
  InputImage
} from '../specs/ImageFaceDetector.nitro'
import type { Face } from '../specs/Face.nitro'
import type { FaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'

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
 * const faces = ImageFaceDetector.detectFaces(uri)
 * ```
 */
export function useImageFaceDetector(
  options?: FaceDetectorOptions
): ImageFaceDetector {
  /**
   * Resolves input image
   * 
   * @param {InputImage} image Image path
   * @returns {string} Resolved image
   */
  function resolveUri(
    image: InputImage
  ): string {
    const uri = ( () => {
      switch ( typeof image ) {
        case 'number': {
          const source = Image.resolveAssetSource( image )
          return source?.uri
        }
        case 'string': {
          return image
        }
        case 'object': {
          return image?.uri
        }
        default: {
          return undefined
        }
      }
    } )()

    if ( !uri ) throw new Error( 'Unable to resolve image' )
    return uri
  }

  return useMemo( () => {
    const imageFaceDetector = createImageFaceDetector( options )

    return {
      ...imageFaceDetector,
      // creates a wrapper to resolve uris before passing to native
      detectFaces( image ) {
        const uri = resolveUri( image )
        return imageFaceDetector.detectFaces( uri )
      }
    }
  }, [ options ] )
}

export default useImageFaceDetector
