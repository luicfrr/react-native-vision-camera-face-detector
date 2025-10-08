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
    var result: [Any] = []

    do {
      var runLandmarks = false
      var runClassifications = false
      var runContours = false
      var trackingEnabled = false
      
      let minFaceSize = 0.15
      let config = common.getConfig(withArguments: options)
      let optionsBuilder = FaceDetectorOptions()
          optionsBuilder.performanceMode = .fast
          optionsBuilder.landmarkMode = .none
          optionsBuilder.contourMode = .none
          optionsBuilder.classificationMode = .none
          optionsBuilder.minFaceSize = minFaceSize
          optionsBuilder.isTrackingEnabled = false

      if config["performanceMode"] as? String == "accurate" {
        optionsBuilder.performanceMode = .accurate
      }

      if config["landmarkMode"] as? String == "all" {
        runLandmarks = true
        optionsBuilder.landmarkMode = .all
      }

      if config["classificationMode"] as? String == "all" {
        runClassifications = true
        optionsBuilder.classificationMode = .all
      }

      if config["contourMode"] as? String == "all" {
        runContours = true
        optionsBuilder.contourMode = .all
      }

      let minFaceSizeParam = config["minFaceSize"] as? Double
      if minFaceSizeParam != nil && minFaceSizeParam != minFaceSize {
        optionsBuilder.minFaceSize = CGFloat(minFaceSizeParam!)
      }

      if config["trackingEnabled"] as? Bool == true {
        trackingEnabled = true
        optionsBuilder.isTrackingEnabled = true
      }

      let faceDetector = FaceDetector.faceDetector(options: optionsBuilder)    
      let image = try self.loadUIImage(from: uri)
      let visionImage = VisionImage(image: image)
      visionImage.orientation = image.imageOrientation

      faceDetector.process(visionImage) { faces, error in
        if let error = error {
          rejecter("E_DETECT", "Error processing image face detection", error)
          return
        }

        for face in faces ?? [] {
          var map: [String: Any] = [:]

          if runLandmarks {
            map["landmarks"] = common.processLandmarks(
              from: face
            )
          }

          if runClassifications {
            map["leftEyeOpenProbability"] = face.leftEyeOpenProbability
            map["rightEyeOpenProbability"] = face.rightEyeOpenProbability
            map["smilingProbability"] = face.smilingProbability
          }

          if runContours {
            map["contours"] = common.processFaceContours(
              from: face
            )
          }

          if trackingEnabled {
            map["trackingId"] = face.trackingID
          }

          map["rollAngle"] = face.headEulerAngleZ
          map["pitchAngle"] = face.headEulerAngleX
          map["yawAngle"] = face.headEulerAngleY
          map["bounds"] = common.processBoundingBox(
            from: face
          )

          result.append(map)
        }

        resolver(result)
      }
    } catch let error {
      rejecter("E_LOAD", "Error preparing face detection: \(error.localizedDescription)", error)
    }
  }
}
