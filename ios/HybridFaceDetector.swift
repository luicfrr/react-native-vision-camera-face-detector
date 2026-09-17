import CoreImage
import NitroModules
import Vision
import VisionCamera

private typealias VisionCameraPoint = margelo.nitro.camera.Point

class HybridFaceDetector: HybridFaceDetectorSpec {
  private let runLandmarks: Bool
  private let runContours: Bool
  private let runClassifications: Bool
  private let trackingEnabled: Bool
  private let autoMode: Bool

  init(_ options: FaceDetectorOptions) {
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    self.autoMode = options.autoMode ?? false
    super.init()
  }

  func detectFaces(frame: any HybridFrameSpec) throws -> [any HybridFaceSpec] {
    guard let nativeFrame = frame as? any NativeFrame else {
      throw RuntimeError.error(withMessage: "Frame is not of type `NativeFrame`!")
    }
    guard let sampleBuffer = nativeFrame.sampleBuffer,
          let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
      throw RuntimeError.error(withMessage: "Frame doesn't contain a valid CVPixelBuffer!")
    }

    let request = VNDetectFaceLandmarksRequest()
    let handler = VNImageRequestHandler(
      cvPixelBuffer: pixelBuffer,
      orientation: frame.orientation.toCGImagePropertyOrientation(isMirrored: frame.isMirrored),
      options: [:]
    )
    try handler.perform([request])

    let config = createFaceProcessConfig(
      frame.width,
      frame.height,
      autoMode,
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled,
      try createFrameToCameraPointTransformer(frame)
    )

    return (request.results ?? []).map { HybridFace(face: $0, config: config) }
  }

  private func createFrameToCameraPointTransformer(
    _ frame: any HybridFrameSpec
  ) throws -> (Double, Double) -> Point {
    let origin = try frame.convertFramePointToCameraPoint(framePoint: VisionCameraPoint(0.0, 0.0))
    let xAxis = try frame.convertFramePointToCameraPoint(framePoint: VisionCameraPoint(1.0, 0.0))
    let yAxis = try frame.convertFramePointToCameraPoint(framePoint: VisionCameraPoint(0.0, 1.0))
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
