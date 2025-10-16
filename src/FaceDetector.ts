import { useMemo } from 'react'
import {
  Platform,
  NativeModules
} from 'react-native'
import {
  VisionCameraProxy,
  type CameraPosition,
  type Frame
} from 'react-native-vision-camera'

type FaceDetectorPlugin = {
  /**
   * Detect faces on frame
   * 
   * @param {Frame} frame Frame to detect faces
   */
  detectFaces: ( frame: Frame ) => Face[]
  /**
   * Stop orientation listeners for Android.
   * Does nothing for IOS.
   * 
   * @returns {void}
   */
  stopListeners: () => void
}

type Point = {
  x: number
  y: number
}

export interface Face {
  pitchAngle: number
  rollAngle: number
  yawAngle: number
  bounds: Bounds
  leftEyeOpenProbability: number
  rightEyeOpenProbability: number
  smilingProbability: number
  contours?: Contours
  landmarks?: Landmarks
  trackingId?: number
}

export interface Bounds {
  width: number
  height: number
  x: number
  y: number
}

export interface Contours {
  FACE: Point[]
  LEFT_EYEBROW_TOP: Point[]
  LEFT_EYEBROW_BOTTOM: Point[]
  RIGHT_EYEBROW_TOP: Point[]
  RIGHT_EYEBROW_BOTTOM: Point[]
  LEFT_EYE: Point[]
  RIGHT_EYE: Point[]
  UPPER_LIP_TOP: Point[]
  UPPER_LIP_BOTTOM: Point[]
  LOWER_LIP_TOP: Point[]
  LOWER_LIP_BOTTOM: Point[]
  NOSE_BRIDGE: Point[]
  NOSE_BOTTOM: Point[]
  LEFT_CHEEK: Point[]
  RIGHT_CHEEK: Point[]
}

export interface Landmarks {
  LEFT_CHEEK: Point
  LEFT_EAR: Point
  LEFT_EYE: Point
  MOUTH_BOTTOM: Point
  MOUTH_LEFT: Point
  MOUTH_RIGHT: Point
  NOSE_BASE: Point
  RIGHT_CHEEK: Point
  RIGHT_EAR: Point
  RIGHT_EYE: Point
}

export interface CommonFaceDetectionOptions {
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

export interface FrameFaceDetectionOptions
  extends CommonFaceDetectionOptions {
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

/**
 * Create a new instance of face detector plugin
 * 
 * @param {FrameFaceDetectionOptions | undefined} options Detection options
 * @returns {FaceDetectorPlugin} Plugin instance
 */
function createFaceDetectorPlugin(
  options?: FrameFaceDetectionOptions
): FaceDetectorPlugin {
  const plugin = VisionCameraProxy.initFrameProcessorPlugin( 'detectFaces', {
    ...options
  } )

  if ( !plugin ) {
    throw new Error( 'Failed to load Frame Processor Plugin "detectFaces"!' )
  }

  return {
    detectFaces: (
      frame: Frame
    ): Face[] => {
      'worklet'
      // @ts-ignore
      return plugin.call( frame ) as Face[]
    },
    stopListeners: () => {
      if ( Platform.OS !== 'android' ) return

      const { VisionCameraFaceDetectorOrientationManager } = NativeModules
      VisionCameraFaceDetectorOrientationManager?.stopDeviceOrientationListener()
    }
  }
}

/**
 * Use an instance of face detector plugin.
 * 
 * @param {FrameFaceDetectionOptions | undefined} options Detection options
 * @returns {FaceDetectorPlugin} Memoized plugin instance that will be 
 * destroyed once the component using `useFaceDetector()` unmounts.
 */
export function useFaceDetector(
  options?: FrameFaceDetectionOptions
): FaceDetectorPlugin {
  return useMemo( () => (
    createFaceDetectorPlugin( options )
  ), [ options ] )
}
