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
  private var outputOrientation: CameraOrientation = .up

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
    image.orientation = outputOrientation.toUIImageOrientation(
      orientationManager.orientation,
      cameraFacing
    )
    let width = CGFloat(frame.height)
    let height = CGFloat(frame.width)
    let config = FaceProcessConfig(
      width: width,
      height: height,
      scaleX: autoMode ? windowWidth / width : 1.0,
      scaleY: autoMode ? windowHeight / height : 1.0,
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
