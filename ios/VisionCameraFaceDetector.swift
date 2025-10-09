import VisionCamera
import Foundation
import MLKitFaceDetection
import MLKitVision
import CoreML
import UIKit
import AVFoundation
import SceneKit

@objc(VisionCameraFaceDetector)
public class VisionCameraFaceDetector: FrameProcessorPlugin {
  enum CameraFacing: String {
    case front = "front"
    case back = "back"
  }
  
  // detection props
  private var autoMode = false
  private var faceDetector: FaceDetector! = nil
  private var runLandmarks = false
  private var runClassifications = false
  private var runContours = false
  private var trackingEnabled = false
  private var windowWidth = 1.0
  private var windowHeight = 1.0
  private var cameraFacing:AVCaptureDevice.Position = .front
  private var common: FaceDetectorCommon! = nil
  private var orientationManager: VisionCameraFaceDetectorOrientation! = nil

  public override init(
    proxy: VisionCameraProxyHolder, 
    options: [AnyHashable : Any]? = [:]
  ) {
    super.init(proxy: proxy, options: options)
    common = FaceDetectorCommon()
    orientationManager = VisionCameraFaceDetectorOrientation()

    let config = common.getConfig(withArguments: options)
    let windowWidthParam = config["windowWidth"] as? Double
    if windowWidthParam != nil && windowWidthParam != windowWidth {
      windowWidth = CGFloat(windowWidthParam!)
    }

    let windowHeightParam = config["windowHeight"] as? Double
    if windowHeightParam != nil && windowHeightParam != windowHeight {
      windowHeight = CGFloat(windowHeightParam!)
    }
    
    if config["cameraFacing"] as? String == "back" {
      cameraFacing = .back
    }

    // handle auto scaling and rotation
    autoMode = config["autoMode"] as? Bool == true
    let faceDetectorResult = common.getFaceDetector(
      config: config
    )
    
    runLandmarks = faceDetectorResult.runLandmarks
    runClassifications = faceDetectorResult.runClassifications
    runContours = faceDetectorResult.runContours
    trackingEnabled = faceDetectorResult.trackingEnabled
    faceDetector = faceDetectorResult.faceDetector
  }

  func getImageOrientation() -> UIImage.Orientation {
    switch orientationManager!.orientation {
      case .portrait:
        return cameraFacing == .front ? .leftMirrored : .right
      case .landscapeLeft:
        return cameraFacing == .front ? .upMirrored : .up
      case .portraitUpsideDown:
        return cameraFacing == .front ? .rightMirrored : .left
      case .landscapeRight:
        return cameraFacing == .front ? .downMirrored : .down
      @unknown default:
        return .up
    }
  }
  
  public override func callback(
    _ frame: Frame, 
    withArguments arguments: [AnyHashable: Any]?
  ) -> Any {
    do {
      // we need to invert sizes as frame is always -90deg rotated
      let width = CGFloat(frame.height)
      let height = CGFloat(frame.width)
      let image = VisionImage(buffer: frame.buffer)
      image.orientation = getImageOrientation()
    
      var scaleX:CGFloat
      var scaleY:CGFloat
      if (autoMode) {
        scaleX = windowWidth / width
        scaleY = windowHeight / height
      } else {
        scaleX = CGFloat(1)
        scaleY = CGFloat(1)
      }

      let faces: [Face] = try faceDetector!.results(in: image)
      return common.processFaces(
        faces: faces,
        runLandmarks: runLandmarks,
        runClassifications: runClassifications,
        runContours: runContours,
        trackingEnabled: trackingEnabled,
        sourceWidth: width,
        sourceHeight: height,
        scaleX: scaleX,
        scaleY: scaleY,
        autoMode: autoMode
      )
    } catch let error {
      print("Error processing face detection: \(error)")
    }

    return []
  }
}
