import {
  VisionCameraProxy,
  type Frame
} from 'react-native-vision-camera'

type DetectFacesType = {
  /** Current frame */
  frame: Frame
  /** Optional callback */
  callback?: CallbackType
  /** Detection options */
  options?: FaceDetectionOptions
}

type Point = {
  x: number
  y: number
}

export type CallbackType = (
  result: DetectionResult
) => void | Promise<void>

export interface DetectionResult {
  faces: Face[]
  frame: FrameData
}

export interface Face {
  pitchAngle: number
  rollAngle: number
  yawAngle: number
  bounds: Bounds
  leftEyeOpenProbability: number
  rightEyeOpenProbability: number
  smilingProbability: number
  contours: Contours
  landmarks: Landmarks
}

export interface FrameData {
  converted?: string
  original?: Frame
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

export interface FaceDetectionOptions {
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
   * @default 0.1
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
   * Should return converted frame as base64?
   * 
   * Note that no frame data will be returned if disabled.
   * 
   * @default false
   */
  convertFrame?: boolean

  /**
   * Should return original frame data?
   * 
   * WARNING: On my tests this freeze frame processor pipeline on IOS.
   * 
   * @default false
   */
  returnOriginal?: boolean
}

const plugin = VisionCameraProxy.initFrameProcessorPlugin( 'detectFaces' )
/**
 * Detect faces on frame
 * 
 * @param {DetectFacesType} props Detect faces prop
 */
export function detectFaces( {
  frame,
  callback,
  options
}: DetectFacesType ): DetectionResult {
  'worklet'
  if ( !plugin ) {
    throw new Error( 'Failed to load Frame Processor Plugin "detectFaces"!' )
  }
  // @ts-ignore
  const result: DetectionResult = plugin.call( frame, options )
  callback?.( result )
  return result
}
