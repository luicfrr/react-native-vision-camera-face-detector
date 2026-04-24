import type { HybridObject } from 'react-native-nitro-modules'
import type { CameraPosition } from 'react-native-vision-camera'
import type { FaceDetector } from './FaceDetector.nitro'

type PerformanceMode = 'fast' | 'accurate'

interface BaseFaceDetectorOptions {
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

  /**
   * Current active camera
   * 
   * @default front
   */
  cameraFacing?: CameraPosition
}

export interface FaceDetectorOptionsAutoModeDisabled
  extends BaseFaceDetectorOptions {
  /**
   * This option must be disabled/undefined if you want to draw on frame using `Skia Frame Processor`.
   * See [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/30#issuecomment-2058805546) and [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/35) for more details. 
   * 
   * @default false
   */
  autoMode?: false
  windowWidth?: number
  windowHeight?: number
}

export interface FaceDetectorOptionsAutoModeEnabled
  extends BaseFaceDetectorOptions {
  /**
   * Should auto scale (face bounds, contour and landmarks) and rotation on native side? 
   * 
   * @default false
   */
  autoMode: true

  /**
   * Required if you want to use `autoMode`. You must handle your own logic to get screen sizes, with or without statusbar size, etc...
   * 
   * @default 1.0
   */
  windowWidth: number

  /**
   * Required if you want to use `autoMode`. You must handle your own logic to get screen sizes, with or without statusbar size, etc...
   * 
   * @default 1.0
   */
  windowHeight: number
}

export type FaceDetectorOptions =
  FaceDetectorOptionsAutoModeEnabled |
  FaceDetectorOptionsAutoModeDisabled

export interface FaceDetectorFactory
  extends HybridObject<{
    ios: 'swift',
    android: 'kotlin'
  }> {
  /**
   * Create a new {@linkcode FaceDetector}.
   */
  createFaceDetector( options: FaceDetectorOptions ): FaceDetector
}
