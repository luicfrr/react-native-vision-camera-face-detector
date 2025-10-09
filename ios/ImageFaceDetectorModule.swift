import Foundation
import React
import MLKitFaceDetection
import MLKitVision
import UIKit

@objc(ImageFaceDetector)
class ImageFaceDetectorModule: NSObject {
  @objc static func requiresMainQueueSetup() -> Bool { false }

  private func loadUIImage(
    from uriString: String
  ) throws -> UIImage {
    if let url = URL(string: uriString) {
      if url.isFileURL {
        if let img = UIImage(contentsOfFile: url.path) { return img }
        throw NSError(domain: "ImageFaceDetector", code: 1, userInfo: [NSLocalizedDescriptionKey: "Could not load image from file path: \(url.path)"])
      }

      // Support bundled assets with no scheme (rare) or http(s) URIs
      if url.scheme == "http" || url.scheme == "https" {
        let data = try Data(contentsOf: url)
        if let img = UIImage(data: data) { return img }
        throw NSError(domain: "ImageFaceDetector", code: 2, userInfo: [NSLocalizedDescriptionKey: "Could not decode image from network data"])
      }

      // Try generic loading via Data for other schemes if possible
      if let data = try? Data(contentsOf: url), let img = UIImage(data: data) {
        return img
      }
    }

    // Fallback: treat string as local path
    if FileManager.default.fileExists(atPath: uriString), let img = UIImage(contentsOfFile: uriString) {
      return img
    }

    throw NSError(domain: "ImageFaceDetector", code: 3, userInfo: [NSLocalizedDescriptionKey: "Unsupported or unreadable uri: \(uriString)"])
  }

  @objc(detectFaces:options:resolver:rejecter:)
  func detectFaces(
    _ uri: String, 
    options: [AnyHashable : Any]? = [:],
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    let common = FaceDetectorCommon()
    do {
      let config = common.getConfig(withArguments: options)
      let faceDetectorResult = common.getFaceDetector(
        config: config
      )
      let runLandmarks = faceDetectorResult.runLandmarks
      let runClassifications = faceDetectorResult.runClassifications
      let runContours = faceDetectorResult.runContours
      let trackingEnabled = faceDetectorResult.trackingEnabled
      let faceDetector = faceDetectorResult.faceDetector
      
      let image = try self.loadUIImage(from: uri)
      let visionImage = VisionImage(image: image)
      visionImage.orientation = image.imageOrientation

      faceDetector.process(visionImage) { faces, error in
        if let error = error {
          print("Error processing image face detection: \(error)")
          // resolve empty list on error
          resolver([])
          return
        }

        let result = common.processFaces(
          faces: faces ?? [],
          runLandmarks: runLandmarks,
          runClassifications: runClassifications,
          runContours: runContours,
          trackingEnabled: trackingEnabled
        )
        resolver(result)
      }
    } catch let error {
      print("Error preparing face detection: \(error)")
      // resolve empty list on error
      resolver([])
    }
  }
}
