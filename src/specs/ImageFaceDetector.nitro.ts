import type { HybridObject } from 'react-native-nitro-modules'
import type { Face } from './Face.nitro'

type InputImage = number | string | { uri: string }
export interface ImageFaceDetector extends HybridObject<{
  ios: 'swift',
  android: 'kotlin'
}> {
  /**
   * Detect faces on image
   * 
   * @param {InputImage} image Image source to detect faces
   */
  detectFaces: ( image: InputImage ) => Promise<Face[]>
}
