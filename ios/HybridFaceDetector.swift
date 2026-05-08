import MLKitFaceDetection
import MLKitVision
import NitroModules
import VisionCamera

class HybridFaceDetector: HybridFaceDetectorSpec {
  private let orientationManager = FaceDetectorOrientation()
  private let runLandmarks: Bool
  private let runContours: Bool
  private let runClassifications: Bool
  private let trackingEnabled: Bool
  private let autoMode: Bool
  private let cameraFacing: CameraPosition
  private let windowWidth: Double
  private let windowHeight: Double
  private let faceDetector: FaceDetector

  init(options: FaceDetectorOptions) {
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    self.autoMode = options.autoMode ?? false
    self.cameraFacing = options.cameraFacing ?? .front
    self.windowWidth = options.windowWidth ?? 1.0
    self.windowHeight = options.windowHeight ?? 1.0
    self.faceDetector = FaceDetector.faceDetector(
      options: options.toMLFaceDetectorOptions()
    )

    super.init()
  }

  func detectFaces(
    frame: any HybridFrameSpec
  ) throws -> [any HybridFaceSpec] {
    let image = try frame.toMLImage()
    let width = Double(image.height)
    let height = Double(image.width)
    let scaleX = autoMode
      ? windowWidth / width
      : 1.0
    let scaleY = autoMode
      ? windowHeight / height
      : 1.0

    let config = FaceProcessConfig(
      width: width,
      height: height,
      scaleX: scaleX,
      scaleY: scaleY,
      runLandmarks: runLandmarks,
      runContours: runContours,
      runClassifications: runClassifications,
      trackingEnabled: trackingEnabled,
      autoMode: autoMode,
      cameraFacing: cameraFacing,
      orientation: orientationManager.orientation
    )

    let faces = try faceDetector.results(in: image)
    return faces.map {
      HybridFace(
        face: $0,
        config: config
      )
    }
  }
}
