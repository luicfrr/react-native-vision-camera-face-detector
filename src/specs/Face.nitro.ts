import type { HybridObject } from 'react-native-nitro-modules'
import type { Bounds } from './Bounds'
import type { Contours } from './Contours'
import type { Landmarks } from './Landmarks'

export interface Face
  extends HybridObject<{
    ios: 'swift'
    android: 'kotlin'
  }> {
  readonly landmarks?: Landmarks
  readonly contours?: Contours
  readonly bounds: Bounds
  readonly leftEyeOpenProbability?: number
  readonly rightEyeOpenProbability?: number
  readonly smilingProbability?: number
  readonly trackingId?: number
  readonly pitchAngle: number
  readonly rollAngle: number
  readonly yawAngle: number
}
