import { NitroModules } from 'react-native-nitro-modules'
import type {
  CameraOutput,
  CameraSession,
  Frame,
} from 'react-native-vision-camera'
import type { Face } from './specs/Face.nitro'
import type { FaceDetector } from './specs/FaceDetector.nitro'
import type {
  FaceDetectorFactory,
  FrameFaceDetectorOptions,
  FaceDetectorOutputOptions,
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

/**
 * Create a new Face Detector {@linkcode CameraOutput}.
 *
 * The Face Detector {@linkcode CameraOutput} can be
 * attached to a {@linkcode CameraSession}.
 */
export function createFaceDetectorOutput(
  options?: FaceDetectorOutputOptions,
): CameraOutput {
  return factory.createFaceDetectorOutput( options )
}
