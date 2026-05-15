import UIKit
import VisionCamera

extension CameraOrientation {
  func toUIImageOrientation(
    _ orientation: UIInterfaceOrientation,
    _ cameraFacing: CameraPosition
  ) -> UIImage.Orientation {
    switch orientation {
      case .portrait: return cameraFacing == .front ? .leftMirrored : .right
      case .landscapeLeft: return cameraFacing == .front ? .downMirrored : .up
      case .portraitUpsideDown: return cameraFacing == .front ? .rightMirrored : .left
      case .landscapeRight: return cameraFacing == .front ? .upMirrored : .down
      default: return .up
    }
  }
}
