import MLKitFaceDetection
import NitroModules
import Foundation
import UIKit

struct FaceProcessConfig {
  let width: Double
  let height: Double
  let scaleX: Double
  let scaleY: Double
  let runLandmarks: Bool
  let runContours: Bool
  let runClassifications: Bool
  let trackingEnabled: Bool
  let autoMode: Bool?
  let cameraFacing: CameraPosition?
  let orientation: UIInterfaceOrientation?

  init(
    width: Double,
    height: Double,
    scaleX: Double,
    scaleY: Double,
    runLandmarks: Bool,
    runContours: Bool,
    runClassifications: Bool,
    trackingEnabled: Bool,
    autoMode: Bool? = nil,
    cameraFacing: CameraPosition? = nil,
    orientation: UIInterfaceOrientation? = nil
  ) {
    self.width = width
    self.height = height
    self.scaleX = scaleX
    self.scaleY = scaleY
    self.runLandmarks = runLandmarks
    self.runContours = runContours
    self.runClassifications = runClassifications
    self.trackingEnabled = trackingEnabled
    self.autoMode = autoMode
    self.cameraFacing = cameraFacing
    self.orientation = orientation
  }
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

  private func transformPoint(
    x: Double,
    y: Double,
    width: Double,
    height: Double
  ) -> (Double, Double) {
    let scaleX = config.scaleX
    let scaleY = config.scaleY

    if config.autoMode != true {
      return (
        y * scaleX,
        x * scaleY
      )
    }

    switch config.cameraFacing {
      case .front:
        switch config.orientation {
          case .portrait:
            return (
              (-(x * scaleX) + config.width * scaleX) - width,
              y * scaleY
            )
          case .landscapeLeft:
            return (
              y * scaleX,
              x * scaleY
            )
          case .portraitUpsideDown:
            return (
              x * scaleX,
              ((-y * scaleY) + config.height * scaleY) - height
            )
          case .landscapeRight:
            return (
              ((-y * scaleX) + config.width * scaleX) - width,
              ((-x * scaleY) + config.height * scaleY) - height
            )
          default:
            return (
              x * scaleX,
              y * scaleY
            )
        }
      default:
        switch config.orientation {
          case .portrait:
            return (
              x * scaleX,
              y * scaleY
            )
          case .landscapeLeft:
            return (
              y * scaleX,
              ((-x * scaleY) + config.height * scaleY) - height
            )
          case .portraitUpsideDown:
            return (
              ((-x * scaleX) + config.width * scaleX) - width,
              ((-y * scaleY) + config.height * scaleY) - height
            )
          case .landscapeRight:
            return (
              ((-y * scaleX) + config.width * scaleX) - width,
              x * scaleY
            )
          default:
            return (
              x * scaleX,
              y * scaleY
            )
        }
    }
  }

  private func processBoundingBox(
    _ boundingBox: CGRect
  ) -> Bounds {
    let scaleX = config.scaleX
    let scaleY = config.scaleY
    let width = boundingBox.width * scaleX
    let height = boundingBox.height * scaleY

    let (x, y) = transformPoint(
      x: boundingBox.origin.x,
      y: boundingBox.origin.y,
      width: width,
      height: height
    )

    return Bounds(
      width: width,
      height: height,
      x: x,
      y: y
    )
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
      let (x, y) = transformPoint(
        x: Double(position.x),
        y: Double(position.y),
        width: 0,
        height: 0
      )

      return Point(
        x: x, 
        y: y
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
        let (x, y) = transformPoint(
          x: Double(point.x),
          y: Double(point.y),
          width: 0,
          height: 0
        )

        return Point(
          x: x, 
          y: y
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
}
