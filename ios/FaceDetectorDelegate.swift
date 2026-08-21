import AVFoundation
import Foundation
import VisionCamera

final class FaceDetectorDelegate: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
  private let onSampleBuffer: (CMSampleBuffer, CameraOrientation, Bool) -> Void

  init(onSampleBuffer: @escaping (CMSampleBuffer, CameraOrientation, Bool) -> Void) {
    self.onSampleBuffer = onSampleBuffer
    super.init()
  }

  func captureOutput(
    _ output: AVCaptureOutput, 
    didOutput sampleBuffer: CMSampleBuffer,
    from connection: AVCaptureConnection
  ) {
    let orientation: CameraOrientation
    switch connection.videoOrientation {
      case .portrait: orientation = .up
      case .portraitUpsideDown: orientation = .down
      case .landscapeRight: orientation = .left
      case .landscapeLeft: orientation = .right
      @unknown default: orientation = .up
    }
    onSampleBuffer(sampleBuffer, orientation, connection.isVideoMirrored)
  }
}
