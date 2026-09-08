import AVFoundation
import UIKit
import VisionCamera

extension CameraOrientation {
  /// Returns this buffer orientation relative to VisionCamera's target output
  /// orientation. This mirrors VisionCamera's internal coordinate metadata
  /// calculation without relying on an internal API from that module.
  func relativeToOutput(_ outputOrientation: CameraOrientation) -> CameraOrientation {
    let relativeDegrees = (degrees - outputOrientation.degrees + 360) % 360
    switch relativeDegrees {
      case 0: return .up
      case 90: return .right
      case 180: return .down
      default: return .left
    }
  }

  func toMLKitImageOrientation(
    isMirrored: Bool
  ) -> UIImage.Orientation {
    switch self {
      case .up: return isMirrored ? .upMirrored : .up
      case .down: return isMirrored ? .downMirrored : .down
      case .left: return isMirrored ? .leftMirrored : .left
      case .right: return isMirrored ? .rightMirrored : .right
    }
  }

  /// Returns the orientation of an unrotated camera buffer for ML Kit.
  ///
  /// `CameraOutput` leaves its AVCaptureVideoDataOutput in the sensor's native
  /// orientation for performance. ML Kit therefore needs the target device
  /// orientation and camera position, rather than the output-to-preview
  /// coordinate transform used later for drawing.
  func toMLKitBufferOrientation(
    cameraPosition: AVCaptureDevice.Position
  ) -> UIImage.Orientation {
    switch (cameraPosition, self) {
      case (.back, .up): return .right
      case (.back, .left): return .up
      case (.back, .down): return .left
      case (.back, .right): return .down
      case (.front, .up): return .leftMirrored
      case (.front, .left): return .downMirrored
      case (.front, .down): return .rightMirrored
      case (.front, .right): return .upMirrored
      default: return .up
    }
  }

  private var degrees: Int {
    switch self {
      case .up: return 0
      case .right: return 90
      case .down: return 180
      case .left: return 270
    }
  }
}
