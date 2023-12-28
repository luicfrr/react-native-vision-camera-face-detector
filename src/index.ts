import { VisionCameraProxy, type Frame } from 'react-native-vision-camera';

const plugin = VisionCameraProxy.initFrameProcessorPlugin('detectFaces');

/**
 * Scans Faces.
 */

type Point = { x: number; y: number };
export interface Face {
  leftEyeOpenProbability: number;
  rollAngle: number;
  pitchAngle: number;
  yawAngle: number;
  rightEyeOpenProbability: number;
  smilingProbability: number;
  bounds: {
    y: number;
    x: number;
    height: number;
    width: number;
    aspectRatio: number;
  };
  contours: Contours;
  landMarks: Landmarks;
}

export interface Contours {
  FACE: Point[];
  NOSE_BOTTOM: Point[];
  LOWER_LIP_TOP: Point[];
  RIGHT_EYEBROW_BOTTOM: Point[];
  LOWER_LIP_BOTTOM: Point[];
  NOSE_BRIDGE: Point[];
  RIGHT_CHEEK: Point[];
  RIGHT_EYEBROW_TOP: Point[];
  LEFT_EYEBROW_TOP: Point[];
  UPPER_LIP_BOTTOM: Point[];
  LEFT_EYEBROW_BOTTOM: Point[];
  UPPER_LIP_TOP: Point[];
  LEFT_EYE: Point[];
  RIGHT_EYE: Point[];
  LEFT_CHEEK: Point[];
}

export interface Landmarks {
  LEFT_CHEEK: Point;
  RIGHT_CHEEK: Point;
  LEFT_EYE: Point;
  RIGHT_EYE: Point;
  LEFT_EAR: Point;
  RIGHT_EAR: Point;
  NOSE_BASE: Point;
  MOUTH_LEFT: Point;
  MOUTH_RIGHT: Point;
  MOUTH_BOTTOM: Point;
}

export interface FaceDetectionOptions {
  /**
   * Favor speed or accuracy when detecting faces.
   *
   * @default 'fast'
   */
  performanceMode?: 'fast' | 'accurate';

  /**
   * Whether to attempt to identify facial "landmarks": eyes, ears, nose, cheeks, mouth, and so on.
   *
   * @default 'none'
   */
  landmarkMode?: 'none' | 'all';

  /**
   * Whether to detect the contours of facial features. Contours are detected for only the most prominent face in an image.
   *
   * @default 'none'
   */
  contourMode?: 'none' | 'all';

  /**
   * Whether or not to classify faces into categories such as "smiling", and "eyes open".
   *
   * @default 'none'
   */
  classificationMode?: 'none' | 'all';

  /**
   * Sets the smallest desired face size, expressed as the ratio of the width of the head to width of the image.
   *
   * @default 0.1
   */
  minFaceSize?: number;

  /**
   * **(COMING SOON)**
   *
   * Whether or not to assign faces an ID, which can be used to track faces across images.
   *
   * Note that when contour detection is enabled, only one face is detected, so face tracking doesn't produce useful results. For this reason, and to improve detection speed, don't enable both contour detection and face tracking.
   *
   * @default false
   */
  trackingEnabled?: boolean;
}

export function scanFaces(frame: Frame, options?: FaceDetectionOptions): Face {
  'worklet';
  if (plugin == null) {
    throw new Error('Failed to load Frame Processor Plugin "scanFaces"!');
  }
  // @ts-ignore
  return plugin.call(frame, options);
}
