import VisionCamera
import AVFoundation
import CoreMotion
import Foundation
import UIKit

final class VisionCameraFaceDetectorOrientation {
  private let motionManager = CMMotionManager()
  private let operationQueue = OperationQueue()
  
  // The orientation of the physical device's gyro sensor/accelerometer
  var orientation: Orientation {
    didSet {
      if oldValue != orientation {
        print("Device Orientation changed from \(oldValue) -> \(orientation)")
      }
    }
  }
    
  init() {
    // default value
    orientation = .portrait
    startDeviceOrientationListener()
  }
    
  deinit {
    stopDeviceOrientationListener()
  }
  
  private func startDeviceOrientationListener() {
    stopDeviceOrientationListener()
    if motionManager.isAccelerometerAvailable {
      motionManager.accelerometerUpdateInterval = 0.2
      motionManager.startAccelerometerUpdates(to: operationQueue) { accelerometerData, error in
        if let error {
          print("Failed to get Accelerometer data! \(error)")
        }
        if let accelerometerData {
          self.orientation = accelerometerData.deviceOrientation
        }
      }
    }
  }
  
  private func stopDeviceOrientationListener() {
    if motionManager.isAccelerometerActive {
      motionManager.stopAccelerometerUpdates()
    }
  }
}

extension CMAccelerometerData {
  /**
   Get the current device orientation from the given acceleration/gyro data.
   */
  var deviceOrientation: Orientation {
    let acceleration = acceleration
    let xNorm = abs(acceleration.x)
    let yNorm = abs(acceleration.y)
    let zNorm = abs(acceleration.z)

    // If the z-axis is greater than the other axes, the phone is flat.
    if zNorm > xNorm && zNorm > yNorm {
      return .portrait
    }

    if xNorm > yNorm {
      if acceleration.x > 0 {
        return .landscapeRight
      } else {
        return .landscapeLeft
      }
    } else {
      if acceleration.y > 0 {
        return .portraitUpsideDown
      } else {
        return .portrait
      }
    }
  }
}
