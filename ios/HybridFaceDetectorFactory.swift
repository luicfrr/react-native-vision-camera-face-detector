import NitroModules
import VisionCamera

class HybridFaceDetectorFactory: HybridFaceDetectorFactorySpec {
  func createFaceDetector(options: FaceDetectorOptions) throws -> any HybridFaceDetectorSpec {
    return HybridFaceDetector(options: options)
  }

  func createFaceDetectorOutput(options: FaceDetectorOutputOptions) throws
    -> any HybridCameraOutputSpec
  {
    return HybridFaceDetectorOutput(options: options)
  }
}
