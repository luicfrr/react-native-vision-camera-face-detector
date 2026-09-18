import AVFoundation
import ImageIO
import UIKit
import VisionCamera

extension CameraOrientation {
  func relativeToOutput(_ outputOrientation: CameraOrientation) -> CameraOrientation {
    let relativeDegrees = (degrees - outputOrientation.degrees + 360) % 360
    switch relativeDegrees {
      case 0: return .up
      case 90: return .right
      case 180: return .down
      default: return .left
    }
  }

  func toCGImagePropertyOrientation(isMirrored: Bool) -> CGImagePropertyOrientation {
    switch self {
      case .up: return isMirrored ? .upMirrored : .up
      case .down: return isMirrored ? .downMirrored : .down
      case .left: return isMirrored ? .leftMirrored : .left
      case .right: return isMirrored ? .rightMirrored : .right
    }
  }

  func toVisionBufferOrientation(
    cameraPosition: AVCaptureDevice.Position
  ) -> CGImagePropertyOrientation {
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
