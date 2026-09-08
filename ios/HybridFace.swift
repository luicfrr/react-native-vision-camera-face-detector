import MLKitFaceDetection
import NitroModules
import AVFoundation
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

// Converts points from an AVCaptureOutput's pixel coordinate system into
// normalized capture-device coordinates.
//
// ML Kit returns face coordinates relative to the raw pixel buffer. Unlike a
// VisionCamera `Frame`, that buffer can have a different physical orientation
// or mirroring from the preview. `AVCaptureOutput` owns the native conversion
// for exactly that boundary, so use it instead of applying the ML image
// orientation a second time.
func createOutputToCameraPointTransformer(
  output: AVCaptureOutput,
  frameWidth: Double,
  frameHeight: Double
) -> (Double, Double) -> Point {
  func convert(
    _ x: Double, 
    _ y: Double
  ) -> CGPoint {
    let rect = output.metadataOutputRectConverted(
      fromOutputRect: CGRect(
        x: CGFloat(x), 
        y: CGFloat(y),
        width: 0, 
        height: 0
      )
    )
    return rect.origin
  }

  // Sample the native affine mapping while handling the sample buffer. Face
  // properties can be read later on another thread, after the output changed.
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
    return Point(
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
