import type { Face } from "./Face.nitro"

export type FaceDetectedCallback = (
  faces: Face[]
) => void | Promise<void>
