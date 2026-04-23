import { NitroModules } from 'react-native-nitro-modules'
import type { Frame } from 'react-native-vision-camera'
import type { Face } from './specs/Face.nitro'
import type { FaceDetector } from './specs/FaceDetector.nitro'
import type {
  FaceDetectorFactory,
  FrameFaceDetectorOptions
} from './specs/FaceDetectorFactory.nitro'

const factory = NitroModules.createHybridObject<FaceDetectorFactory>(
  'FaceDetectorFactory',
)

/**
 * Create a new {@linkcode FaceDetector}.
 *
 * The {@linkcode FaceDetector} can be used to
 * scan {@linkcode Face}s in a {@linkcode Frame}.
 */
export function createFaceDetector(
  options?: FrameFaceDetectorOptions
): FaceDetector {
  return factory.createFaceDetector( options )
}
