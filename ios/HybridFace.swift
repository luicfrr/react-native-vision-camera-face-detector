import MLKitFaceDetection
import NitroModules
import Foundation
import UIKit

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
  return { x, y in Point(x: x, y: y) }
}

/// Converts raw CMSampleBuffer coordinates into VisionCamera camera coordinates.
/// ML Kit keeps its result coordinates in the raw buffer coordinate system even
/// when `MLImage.orientation` is set, so rotation has to be applied here.
func createFrameToCameraPointTransformer(
  frameWidth: Double,
  frameHeight: Double,
  orientation: UIImage.Orientation
) -> (Double, Double) -> Point {
  let isMirrored: Bool
  let rotation: UIImage.Orientation
  switch orientation {
    case .upMirrored:
      isMirrored = true
      rotation = .up
    case .downMirrored:
      isMirrored = true
      rotation = .down
    case .leftMirrored:
      isMirrored = true
      rotation = .left
    case .rightMirrored:
      isMirrored = true
      rotation = .right
    default:
      isMirrored = false
      rotation = orientation
  }

  return { x, y in
    let normalizedX = (isMirrored ? frameWidth - x : x) / frameWidth
    let normalizedY = y / frameHeight

    switch rotation {
      case .down:
        return Point(x: 1.0 - normalizedX, y: 1.0 - normalizedY)
      case .left:
        return Point(x: 1.0 - normalizedY, y: normalizedX)
      case .right:
        return Point(x: normalizedY, y: 1.0 - normalizedX)
      default:
        return Point(x: normalizedX, y: normalizedY)
    }
  }
}

func createFaceProcessConfig(
  _ frameWidth: Double,
  _ frameHeight: Double,
  _ autoMode: Bool,
  _ pointTransformer: @escaping (Double, Double) -> Point,
  _ runLandmarks: Bool,
  _ runContours: Bool,
  _ runClassifications: Bool,
  _ trackingEnabled: Bool
) -> FaceProcessConfig {
  return FaceProcessConfig(
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
  private let face: Face
  private let config: FaceProcessConfig

  init(
    face: Face,
    config: FaceProcessConfig
  ) {
    self.face = face
    self.config = config
    super.init()
  }

  private func processBoundingBox(
    _ boundingBox: CGRect
  ) -> Bounds {
    let points = [
      transformPoint(x: Double(boundingBox.minX), y: Double(boundingBox.minY)),
      transformPoint(x: Double(boundingBox.maxX), y: Double(boundingBox.minY)),
      transformPoint(x: Double(boundingBox.minX), y: Double(boundingBox.maxY)),
      transformPoint(x: Double(boundingBox.maxX), y: Double(boundingBox.maxY))
    ]
    let minX = points.map(\.x).min() ?? 0.0
    let maxX = points.map(\.x).max() ?? 0.0
    let minY = points.map(\.y).min() ?? 0.0
    let maxY = points.map(\.y).max() ?? 0.0

    return Bounds(
      width: maxX - minX,
      height: maxY - minY,
      x: minX,
      y: minY
    )
  }

  private func transformPoint(
    x: Double,
    y: Double
  ) -> Point {
    return config.pointTransformer(x, y)
  }

  private func processLandmarks(
      _ face: Face
  ) -> Landmarks {
    func getPoint(
      _ type: FaceLandmarkType
    ) -> Point? {
      guard let landmark = face.landmark(ofType: type) else {
        return nil
      }

      let position = landmark.position
      return transformPoint(
        x: Double(position.x),
        y: Double(position.y)
      )
    }

    return Landmarks(
      LEFT_CHEEK: getPoint(.leftCheek),
      LEFT_EAR: getPoint(.leftEar),
      LEFT_EYE: getPoint(.leftEye),
      MOUTH_BOTTOM: getPoint(.mouthBottom),
      MOUTH_LEFT: getPoint(.mouthLeft),
      MOUTH_RIGHT: getPoint(.mouthRight),
      NOSE_BASE: getPoint(.noseBase),
      RIGHT_CHEEK: getPoint(.rightCheek),
      RIGHT_EAR: getPoint(.rightEar),
      RIGHT_EYE: getPoint(.rightEye)
    )
  }

  private func processFaceContours(
    _ face: Face
  ) -> Contours {
    func getContour(
        _ type: FaceContourType
    ) -> [Point]? {
      guard let contour = face.contour(ofType: type) else {
        return nil
      }

      return contour.points.map { point in
        return transformPoint(
          x: Double(point.x),
          y: Double(point.y)
        )
      }
    }

    return Contours(
      FACE: getContour(.face),
      LEFT_EYEBROW_TOP: getContour(.leftEyebrowTop),
      LEFT_EYEBROW_BOTTOM: getContour(.leftEyebrowBottom),
      RIGHT_EYEBROW_TOP: getContour(.rightEyebrowTop),
      RIGHT_EYEBROW_BOTTOM: getContour(.rightEyebrowBottom),
      LEFT_EYE: getContour(.leftEye),
      RIGHT_EYE: getContour(.rightEye),
      UPPER_LIP_TOP: getContour(.upperLipTop),
      UPPER_LIP_BOTTOM: getContour(.upperLipBottom),
      LOWER_LIP_TOP: getContour(.lowerLipTop),
      LOWER_LIP_BOTTOM: getContour(.lowerLipBottom),
      NOSE_BRIDGE: getContour(.noseBridge),
      NOSE_BOTTOM: getContour(.noseBottom),
      LEFT_CHEEK: getContour(.leftCheek),
      RIGHT_CHEEK: getContour(.rightCheek)
    )
  }

  var bounds: Bounds {
    processBoundingBox(face.frame)
  }

  var landmarks: Landmarks? {
    config.runLandmarks ?
    processLandmarks(face): nil
  }

  var contours: Contours? {
    config.runContours ?
    processFaceContours(face) : nil
  }

  var leftEyeOpenProbability: Double? {
    config.runClassifications ?
    face.leftEyeOpenProbability : nil
  }

  var rightEyeOpenProbability: Double? {
    config.runClassifications ?
    face.rightEyeOpenProbability : nil
  }

  var smilingProbability: Double? {
    config.runClassifications ?
    face.smilingProbability : nil
  }

  var trackingId: Double? {
    config.trackingEnabled ?
    Double(face.trackingID) : nil
  }

  var pitchAngle: Double {
    return face.headEulerAngleX
  }

  var rollAngle: Double {
    return face.headEulerAngleZ
  }

  var yawAngle: Double {
    return face.headEulerAngleY
  }

  var frameWidth: Double {
    return config.frameWidth
  }

  var frameHeight: Double {
    return config.frameHeight
  }
}
