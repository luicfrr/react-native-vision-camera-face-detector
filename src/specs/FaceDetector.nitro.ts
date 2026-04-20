import { type Frame } from 'react-native-vision-camera'
import { type HybridObject } from 'react-native-nitro-modules'
import type {
  Face,
  FrameFaceDetectionOptions
} from '../FaceDetector'

export interface FaceDetectorPlugin extends HybridObject<{
  ios: 'swift',
  android: 'kotlin'
}> {
  /**
   * Initializ
   * 
   * @param options 
   * @returns 
   */
  setup: ( options?: FrameFaceDetectionOptions ) => void
  /**
   * Detect faces on frame
   * 
   * @param {Frame} frame Frame to detect faces
   */
  detectFaces: ( frame: Frame ) => Promise<Face[]>
  /**
   * Stop orientation listeners for Android.
   * Does nothing for IOS.
   * 
   * @returns {void}
   */
  stopListeners: () => void
}
