import { NitroModules } from 'react-native-nitro-modules'
import type { Frame } from 'react-native-vision-camera'
import type { Face } from './specs/Face.nitro'
import type { FaceDetector } from './specs/FaceDetector.nitro'
import type { ImageFaceDetector } from './specs/ImageFaceDetector.nitro'
import type {
  FaceDetectorFactory,
  FaceDetectorOptions
} from './specs/FaceDetectorFactory.nitro'
import type { ImageFaceDetectorFactory } from './specs/ImageFaceDetectorFactory.nitro'

const faceDetectorFactory = NitroModules.createHybridObject<FaceDetectorFactory>(
  'FaceDetectorFactory'
)

const imageFaceDetectorFactory = NitroModules.createHybridObject<ImageFaceDetectorFactory>(
  'ImageFaceDetectorFactory'
)

/**
 * Create a new {@linkcode FaceDetector}.
 *
 * The {@linkcode FaceDetector} can be used to
 * scan {@linkcode Face}s in a {@linkcode Frame}.
 */
export function createFaceDetector(
  options?: FaceDetectorOptions
): FaceDetector {
  return faceDetectorFactory.createFaceDetector( options ?? {} )
}

/**
 * Create a new image {@linkcode ImageFaceDetector}.
 *
 * The {@linkcode ImageFaceDetector} can be used to
 * scan {@linkcode Face}s in a {@linkcode Frame}.
 */
export function createImageFaceDetector(
  options?: FaceDetectorOptions
): ImageFaceDetector {
  return imageFaceDetectorFactory.createImageFaceDetector( options ?? {} )
}
