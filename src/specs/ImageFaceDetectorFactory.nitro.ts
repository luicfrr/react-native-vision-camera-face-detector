import type { HybridObject } from 'react-native-nitro-modules'
import type { ImageFaceDetector } from './ImageFaceDetector.nitro'
import type { FaceDetectorOptions } from './FaceDetectorFactory.nitro'

export interface ImageFaceDetectorFactory
  extends HybridObject<{
    ios: 'swift',
    android: 'kotlin'
  }> {
  /**
  * Create a new {@linkcode ImageFaceDetector}.
  */
  createImageFaceDetector( options: FaceDetectorOptions ): ImageFaceDetector
}
