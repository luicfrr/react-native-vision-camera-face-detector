import {
  Image,
  NativeModules
} from 'react-native'
import type {
  Face,
  CommonFaceDetectionOptions
} from './FaceDetector'

type InputImage = number | string | { uri: string }
export interface ImageFaceDetectionOptions {
  image: InputImage,
  options?: CommonFaceDetectionOptions
}

/**
 * Resolves input image
 * 
 * @param {InputImage} image Image path
 * @returns {string} Resolved image
 */
function resolveUri( image: InputImage ): string {
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

/**
 * Detect faces in a static image
 * 
 * @param {InputImage} image Image path
 * @returns {Promise<Face[]>} List of detected faces
 */
export async function detectFaces( {
  image,
  options
}: ImageFaceDetectionOptions ): Promise<Face[]> {
  const uri = resolveUri( image )
  // @ts-ignore
  const { ImageFaceDetector } = NativeModules
  return await ImageFaceDetector?.detectFaces(
    uri,
    options
  )
}
