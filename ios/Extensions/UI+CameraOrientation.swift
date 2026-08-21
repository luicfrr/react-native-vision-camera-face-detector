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

  private var degrees: Int {
    switch self {
      case .up: return 0
      case .right: return 90
      case .down: return 180
      case .left: return 270
    }
  }
}
