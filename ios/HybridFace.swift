import AVFoundation
import Foundation
import NitroModules
import Vision

struct FaceProcessConfig {
  let frameWidth: Double
  let frameHeight: Double
  let pointTransformer: (Double, Double) -> Point
  let runLandmarks: Bool
  let runContours: Bool
  let runClassifications: Bool
  let trackingEnabled: Bool
}

func createIdentityPointTransformer() -> (Double, Double) -> Point {
  { x, y in Point(x: x, y: y) }
}

func createOutputToCameraPointTransformer(
  output: AVCaptureOutput,
  frameWidth: Double,
  frameHeight: Double
) -> (Double, Double) -> Point {
  func convert(_ x: Double, _ y: Double) -> CGPoint {
    output.metadataOutputRectConverted(
      fromOutputRect: CGRect(x: CGFloat(x), y: CGFloat(y), width: 0, height: 0)
    ).origin
  }

  let origin = convert(0, 0)
  let xAxis = convert(frameWidth, 0)
  let yAxis = convert(0, frameHeight)
  let originX = Double(origin.x)
  let originY = Double(origin.y)
  let xAxisDeltaX = Double(xAxis.x - origin.x) / frameWidth
  let xAxisDeltaY = Double(xAxis.y - origin.y) / frameWidth
  let yAxisDeltaX = Double(yAxis.x - origin.x) / frameHeight
  let yAxisDeltaY = Double(yAxis.y - origin.y) / frameHeight

  return { x, y in
    Point(
      x: originX + xAxisDeltaX * x + yAxisDeltaX * y,
      y: originY + xAxisDeltaY * x + yAxisDeltaY * y
    )
  }
}

func createFaceProcessConfig(
  _ frameWidth: Double,
  _ frameHeight: Double,
  _ autoMode: Bool,
  _ runLandmarks: Bool,
  _ runContours: Bool,
  _ runClassifications: Bool,
  _ trackingEnabled: Bool,
  _ pointTransformer: @escaping (Double, Double) -> Point
) -> FaceProcessConfig {
  FaceProcessConfig(
    frameWidth: frameWidth,
    frameHeight: frameHeight,
    pointTransformer: autoMode ? pointTransformer : createIdentityPointTransformer(),
    runLandmarks: runLandmarks,
    runContours: runContours,
    runClassifications: runClassifications,
    trackingEnabled: trackingEnabled
  )
}

final class HybridFace: HybridFaceSpec {
  private let face: VNFaceObservation
  private let config: FaceProcessConfig

  init(face: VNFaceObservation, config: FaceProcessConfig) {
    self.face = face
    self.config = config
    super.init()
  }

  // Vision uses normalized coordinates with a bottom-left origin. The package's
  // public API uses image pixels with a top-left origin, matching the old ML Kit implementation.
  private func imagePoint(normalizedX x: Double, normalizedY y: Double) -> Point {
    config.pointTransformer(x * config.frameWidth, (1.0 - y) * config.frameHeight)
  }

  private func landmarkPoints(_ region: VNFaceLandmarkRegion2D?) -> [Point]? {
    guard let region else { return nil }
    let box = face.boundingBox
    return region.normalizedPoints.map { point in
      let x = Double(box.minX + CGFloat(point.x) * box.width)
      let y = Double(box.minY + CGFloat(point.y) * box.height)
      return imagePoint(normalizedX: x, normalizedY: y)
    }
  }

  private func center(_ region: VNFaceLandmarkRegion2D?) -> Point? {
    guard let points = landmarkPoints(region), !points.isEmpty else { return nil }
    return Point(
      x: points.map(\.x).reduce(0, +) / Double(points.count),
      y: points.map(\.y).reduce(0, +) / Double(points.count)
    )
  }

  // Apple Vision does not expose ML Kit's eye-open classifier. Estimate eye
  // openness from the eye landmark's vertical-to-horizontal aspect ratio and
  // normalize it to the package's existing 0...1 probability-shaped API.
  // This is an estimate, not an ML Kit-equivalent confidence value.
  private func eyeOpenProbability(_ region: VNFaceLandmarkRegion2D?) -> Double? {
    guard let region, region.pointCount >= 6 else { return nil }

    let points = region.normalizedPoints
    guard
      let minX = points.map({ Double($0.x) }).min(),
      let maxX = points.map({ Double($0.x) }).max(),
      let minY = points.map({ Double($0.y) }).min(),
      let maxY = points.map({ Double($0.y) }).max()
    else {
      return nil
    }

    let width = maxX - minX
    let height = maxY - minY
    guard width > .ulpOfOne else { return nil }

    let aspectRatio = height / width
    let closedAspectRatio = 0.08
    let openAspectRatio = 0.28
    let normalized = (aspectRatio - closedAspectRatio) / (openAspectRatio - closedAspectRatio)

    return min(max(normalized, 0.0), 1.0)
  }

  var bounds: Bounds {
    let box = face.boundingBox
    let topLeft = imagePoint(normalizedX: Double(box.minX), normalizedY: Double(box.maxY))
    let bottomRight = imagePoint(normalizedX: Double(box.maxX), normalizedY: Double(box.minY))
    return Bounds(
      width: abs(bottomRight.x - topLeft.x),
      height: abs(bottomRight.y - topLeft.y),
      x: min(topLeft.x, bottomRight.x),
      y: min(topLeft.y, bottomRight.y)
    )
  }

  var landmarks: Landmarks? {
    guard config.runLandmarks, let l = face.landmarks else { return nil }
    return Landmarks(
      LEFT_CHEEK: center(l.leftPupil ?? l.leftEye),
      LEFT_EAR: nil,
      LEFT_EYE: center(l.leftPupil ?? l.leftEye),
      MOUTH_BOTTOM: center(l.outerLips),
      MOUTH_LEFT: landmarkPoints(l.outerLips)?.first,
      MOUTH_RIGHT: landmarkPoints(l.outerLips)?.last,
      NOSE_BASE: center(l.noseCrest ?? l.nose),
      RIGHT_CHEEK: center(l.rightPupil ?? l.rightEye),
      RIGHT_EAR: nil,
      RIGHT_EYE: center(l.rightPupil ?? l.rightEye)
    )
  }

  var contours: Contours? {
    guard config.runContours, let l = face.landmarks else { return nil }
    return Contours(
      FACE: landmarkPoints(l.faceContour),
      LEFT_EYEBROW_TOP: landmarkPoints(l.leftEyebrow),
      LEFT_EYEBROW_BOTTOM: landmarkPoints(l.leftEyebrow),
      RIGHT_EYEBROW_TOP: landmarkPoints(l.rightEyebrow),
      RIGHT_EYEBROW_BOTTOM: landmarkPoints(l.rightEyebrow),
      LEFT_EYE: landmarkPoints(l.leftEye),
      RIGHT_EYE: landmarkPoints(l.rightEye),
      UPPER_LIP_TOP: landmarkPoints(l.outerLips),
      UPPER_LIP_BOTTOM: landmarkPoints(l.innerLips),
      LOWER_LIP_TOP: landmarkPoints(l.innerLips),
      LOWER_LIP_BOTTOM: landmarkPoints(l.outerLips),
      NOSE_BRIDGE: landmarkPoints(l.noseCrest),
      NOSE_BOTTOM: landmarkPoints(l.nose),
      LEFT_CHEEK: landmarkPoints(l.medianLine),
      RIGHT_CHEEK: landmarkPoints(l.medianLine)
    )
  }

  var leftEyeOpenProbability: Double? {
    guard config.runClassifications else { return nil }
    return eyeOpenProbability(face.landmarks?.leftEye)
  }

  var rightEyeOpenProbability: Double? {
    guard config.runClassifications else { return nil }
    return eyeOpenProbability(face.landmarks?.rightEye)
  }

  // Apple Vision does not expose ML Kit's smile classifier.
  var smilingProbability: Double? { nil }

  // VNFaceObservation has a UUID but no stable numeric tracking identifier.
  var trackingId: Double? { nil }

  // Vision returns radians. Preserve the package's existing degree-based contract.
  var pitchAngle: Double { (face.pitch?.doubleValue ?? 0) * 180.0 / .pi }
  var rollAngle: Double { (face.roll?.doubleValue ?? 0) * 180.0 / .pi }
  var yawAngle: Double { (face.yaw?.doubleValue ?? 0) * 180.0 / .pi }

  var frameWidth: Double { config.frameWidth }
  var frameHeight: Double { config.frameHeight }
}
