import MLKitFaceDetection
import MLKitVision
import NitroModules
import VisionCamera

private typealias VisionCameraPoint = margelo.nitro.camera.Point

class HybridFaceDetector: HybridFaceDetectorSpec {
  private let runLandmarks: Bool
  private let runContours: Bool
  private let runClassifications: Bool
  private let trackingEnabled: Bool
  private let autoMode: Bool
  private let faceDetector: FaceDetector

  init(_ options: FaceDetectorOptions) {
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    self.autoMode = options.autoMode ?? false
    self.faceDetector = FaceDetector.faceDetector(
      options: options.toMLFaceDetectorOptions()
    )

    super.init()
  }

  func detectFaces(
    frame: any HybridFrameSpec
  ) throws -> [any HybridFaceSpec] {
    let image = try frame.toMLImage()

    let config = createFaceProcessConfig(
      frame.width,
      frame.height,
      autoMode,
      try createFrameToCameraPointTransformer(frame),
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled
    )

    let faces = try faceDetector.results(in: image)
    return faces.map {
      HybridFace(
        face: $0,
        config: config
      )
    }
  }

  private func createFrameToCameraPointTransformer(
    _ frame: any HybridFrameSpec
  ) throws -> (Double, Double) -> Point {
    let origin = try frame.convertFramePointToCameraPoint(
      framePoint: VisionCameraPoint(0.0, 0.0)
    )
    let xAxis = try frame.convertFramePointToCameraPoint(
      framePoint: VisionCameraPoint(1.0, 0.0)
    )
    let yAxis = try frame.convertFramePointToCameraPoint(
      framePoint: VisionCameraPoint(0.0, 1.0)
    )

    let originX = origin.x
    let originY = origin.y
    let xAxisDeltaX = xAxis.x - originX
    let xAxisDeltaY = xAxis.y - originY
    let yAxisDeltaX = yAxis.x - originX
    let yAxisDeltaY = yAxis.y - originY

    return { x, y in
      Point(
        x: originX + xAxisDeltaX * x + yAxisDeltaX * y,
        y: originY + xAxisDeltaY * x + yAxisDeltaY * y
      )
    }
  }
}
