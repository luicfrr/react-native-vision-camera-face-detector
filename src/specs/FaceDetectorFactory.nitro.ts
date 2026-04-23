import type { HybridObject } from 'react-native-nitro-modules'
import type {
  CameraOutput,
  CameraPosition
} from 'react-native-vision-camera'
import type { FaceDetector } from './FaceDetector.nitro'
import type { FaceDetectedCallback } from './FaceDetectedCallback'
import type { FaceDetectorOutputResolution } from './FaceDetectorOutputResolution'

export interface CommonFaceDetectorOptions {
  /**
   * Favor speed or accuracy when detecting faces.
   *
   * @default 'fast'
   */
  performanceMode?: 'fast' | 'accurate'

  /**
   * Whether to attempt to identify facial 'landmarks': eyes, ears, nose, cheeks, mouth, and so on.
   *
   * @default 'none'
   */
  landmarkMode?: 'none' | 'all'

  /**
   * Whether to detect the contours of facial features. Contours are detected for only the most prominent face in an image.
   *
   * @default 'none'
   */
  contourMode?: 'none' | 'all'

  /**
   * Whether or not to classify faces into categories such as 'smiling', and 'eyes open'.
   *
   * @default 'none'
   */
  classificationMode?: 'none' | 'all'

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

export interface FrameFaceDetectorOptions
  extends CommonFaceDetectorOptions {
  /**
   * Current active camera
   * 
   * @default front
   */
  cameraFacing?: CameraPosition

  /**
   * Should handle auto scale (face bounds, contour and landmarks) and rotation on native side? 
   * This option should be disabled if you want to draw on frame using `Skia Frame Processor`.
   * See [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/30#issuecomment-2058805546) and [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/35) for more details. 
   * 
   * @default false
   */
  autoMode?: boolean

  /**
   * Required if you want to use `autoMode`. You must handle your own logic to get screen sizes, with or without statusbar size, etc...
   * 
   * @default 1.0
   */
  windowWidth?: number

  /**
   * Required if you want to use `autoMode`. You must handle your own logic to get screen sizes, with or without statusbar size, etc...
   * 
   * @default 1.0
   */
  windowHeight?: number
}

export interface FaceDetectorOutputOptions
  extends FrameFaceDetectorOptions {
  /**
   * Controls which camera buffer resolution should be used.
   *
   * - `'preview'`: Prefer preview-sized buffers for lower latency.
   * - `'full'`: Prefer full/highest available buffers for better detail.
   *
   * @default 'preview'
   */
  outputResolution?: FaceDetectorOutputResolution
  /**
   * Called whenever faces have been detected.
   */
  onFaceDetected: FaceDetectedCallback
  /**
   * Called when there was an error detecting faces.
   */
  onError: ( error: Error ) => void
}

export interface FaceDetectorFactory
  extends HybridObject<{
    ios: 'swift',
    android: 'kotlin'
  }> {
  /**
   * Create a new {@linkcode FaceDetector}.
   */
  createFaceDetector( options?: FrameFaceDetectorOptions ): FaceDetector

  // TODO: Nitro does not support external inheritance in Swift yet, so
  //       we cannot have a custom HybridObject that extends CameraOutput.
  //       Once Nitro supports this, we can return a concrete type here.
  /**
   * Create a new {@linkcode CameraOutput} that can
   * detect Barcodes.
   */
  createFaceDetectorOutput( options?: FaceDetectorOutputOptions ): CameraOutput
}
