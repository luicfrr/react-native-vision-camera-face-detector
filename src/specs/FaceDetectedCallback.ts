import type { Frame } from "react-native-vision-camera"
import type { Face } from "./Face.nitro"

export type FaceDetectedCallback = (
  faces: Face[],
  frame: Frame
) => void | Promise<void>
