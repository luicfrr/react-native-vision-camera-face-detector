import type { Point } from "react-native-vision-camera"

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
