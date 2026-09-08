import type {
  CameraOutput,
  CameraPosition,
  MirrorMode,
} from 'react-native-vision-camera'
import type { HybridObject } from 'react-native-nitro-modules'
import type { Face } from './Face.nitro'
import type { FaceDetector } from './FaceDetector.nitro'
import type { ImageFaceDetectorOptions } from './ImageFaceDetectorFactory.nitro'

export interface FaceDetectorOptions extends ImageFaceDetectorOptions {
  /**
   * Current active camera
   *
   * @default front
   */
  cameraFacing?: CameraPosition

  /**
   * VisionCamera's `mirrorMode` prop.
   *
   * @default auto
   */
  mirrorMode?: MirrorMode

  /**
   * Converts face bounds, contours and landmarks from frame coordinates into
   * VisionCamera camera coordinates. When using this package's `<Camera />`,
   * those coordinates are then converted to its preview view automatically.
   *
   * When using `useFaceDetectorOutput()` directly, use the same `mirrorMode`
   * on the detector output and Camera, then convert camera coordinates with
   * `cameraRef.current.convertCameraPointToViewPoint(...)` before drawing in a
   * preview. Disable this option when drawing directly in a frame processor
   * with `useFaceDetector()`.
   * See [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/30#issuecomment-2058805546) and [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/35) for more details.
   *
   * @default false
   */
  autoMode?: boolean
}

/**
 * Controls the camera buffer resolution used for barcode scanning.
 *
 * - `'preview'`: Prefer preview-sized buffers for lower latency.
 * - `'full'`: Prefer full/highest available buffers for better detail.
 */
export type FaceDetectorOutputResolution = 'preview' | 'full'

export interface FaceDetectorOutputOptions extends FaceDetectorOptions {
  /**
   * Controls which camera buffer resolution should be used.
   *
   * - `'preview'`: Prefer preview-sized buffers for lower latency.
   * - `'full'`: Prefer full/highest available buffers for better detail.
   *
   * @default 'preview'
   */
  outputResolution?: FaceDetectorOutputResolution

  /**
   * Called whenever faces have been detected.
   */
  onFacesDetected: ( faces: Face[] ) => void

  /**
   * Called when there was an error detecting faces.
   */
  onError: ( error: Error ) => void
}

export interface FaceDetectorFactory extends HybridObject<{
  ios: 'swift'
  android: 'kotlin'
}> {
  /**
   * Create a new {@linkcode FaceDetector}.
   */
  createFaceDetector( options: FaceDetectorOptions ): FaceDetector

  /**
   * Create a new {@linkcode CameraOutput} that can
   * detect Barcodes.
   */
  createFaceDetectorOutput( options: FaceDetectorOutputOptions ): CameraOutput
}
