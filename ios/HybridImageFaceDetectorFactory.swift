import NitroModules
import VisionCamera

class HybridImageFaceDetectorFactory: HybridImageFaceDetectorFactorySpec {
  func createImageFaceDetector(options: ImageFaceDetectorOptions) throws -> any HybridImageFaceDetectorSpec {
    return HybridImageFaceDetector(options)
  }
}
