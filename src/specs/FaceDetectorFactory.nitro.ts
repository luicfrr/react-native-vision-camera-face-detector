import type { HybridObject } from 'react-native-nitro-modules'
import type { CameraPosition } from 'react-native-vision-camera'
import type { FaceDetector } from './FaceDetector.nitro'
import type { ImageFaceDetectorOptions } from './ImageFaceDetectorFactory.nitro'

export interface FaceDetectorOptions
  extends ImageFaceDetectorOptions {
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

export interface FaceDetectorFactory
  extends HybridObject<{
    ios: 'swift',
    android: 'kotlin'
  }> {
  /**
   * Create a new {@linkcode FaceDetector}.
   */
  createFaceDetector( options?: FaceDetectorOptions ): FaceDetector
}
