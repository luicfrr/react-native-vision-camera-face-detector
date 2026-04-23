import type { HybridObject } from 'react-native-nitro-modules'
import type { ImageFaceDetector } from './ImageFaceDetector.nitro'

type PerformanceMode = 'fast' | 'accurate'

export interface ImageFaceDetectorOptions {
  /**
   * Favor speed or accuracy when detecting faces.
   *
   * @default 'fast'
   */
  performanceMode?: PerformanceMode

  /**
   * Whether to attempt to identify facial 'landmarks': eyes, ears, nose, cheeks, mouth, and so on.
   *
   * @default false
   */
  runLandmarks?: boolean

  /**
   * Whether to detect the contours of facial features. Contours are detected for only the most prominent face in an image.
   *
   * @default false
   */
  runContours?: boolean

  /**
   * Whether or not to classify faces into categories such as 'smiling', and 'eyes open'.
   *
   * @default false
   */
  runClassifications?: boolean

  /**
   * Sets the smallest desired face size, expressed as the ratio of the width of the head to width of the image.
   *
   * @default 0.15
   */
  minFaceSize?: number

  /**
   * Whether or not to assign faces an ID, which can be used to track faces across images.
   *
   * Note that when contour detection is enabled, only one face is detected, so face tracking doesn't produce useful results. For this reason, and to improve detection speed, don't enable both contour detection and face tracking.
   *
   * @default false
   */
  trackingEnabled?: boolean
}

export interface ImageFaceDetectorFactory
  extends HybridObject<{
    ios: 'swift',
    android: 'kotlin'
  }> {
  /**
  * Create a new Image {@linkcode FaceDetector}.
  */
  createImageFaceDetector( options?: ImageFaceDetectorOptions ): ImageFaceDetector
}
