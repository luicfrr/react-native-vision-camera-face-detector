import Foundation
import React
import MLKitFaceDetection
import MLKitVision
import UIKit

@objc(ImageFaceDetector)
class ImageFaceDetector: NSObject {
  @objc static func requiresMainQueueSetup() -> Bool { false }

  private func loadUIImage(from uriString: String) throws -> UIImage {
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

  @objc(hasFaceInImage:resolver:rejecter:)
  func hasFaceInImage(_ uri: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.global(qos: .userInitiated).async {
      do {
        let image = try self.loadUIImage(from: uri)
        let visionImage = VisionImage(image: image)
        visionImage.orientation = image.imageOrientation

        let options = FaceDetectorOptions()
        options.performanceMode = .fast
        options.landmarkMode = .none
        options.contourMode = .none
        options.classificationMode = .none
        options.minFaceSize = 0.15
        options.isTrackingEnabled = false

        let detector = FaceDetector.faceDetector(options: options)
        do {
          let faces = try detector.results(in: visionImage)
          resolver(!faces.isEmpty)
        } catch let error {
          rejecter("E_DETECT", "Failed to run face detection", error)
        }
      } catch let error {
        rejecter("E_LOAD", "Failed to load image: \(error.localizedDescription)", error)
      }
    }
  }

  @objc(countFacesInImage:resolver:rejecter:)
  func countFacesInImage(_ uri: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.global(qos: .userInitiated).async {
      do {
        let image = try self.loadUIImage(from: uri)
        let visionImage = VisionImage(image: image)
        visionImage.orientation = image.imageOrientation

        let options = FaceDetectorOptions()
        options.performanceMode = .fast
        options.landmarkMode = .none
        options.contourMode = .none
        options.classificationMode = .none
        options.minFaceSize = 0.15
        options.isTrackingEnabled = false

        let detector = FaceDetector.faceDetector(options: options)
        do {
          let faces = try detector.results(in: visionImage)
          resolver(faces.count)
        } catch let error {
          rejecter("E_DETECT", "Failed to run face detection", error)
        }
      } catch let error {
        rejecter("E_LOAD", "Failed to load image: \(error.localizedDescription)", error)
      }
    }
  }
}
