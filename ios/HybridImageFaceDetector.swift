import Foundation
import NitroModules
import UIKit
import Vision

class HybridImageFaceDetector: HybridImageFaceDetectorSpec {
  private let runLandmarks: Bool
  private let runContours: Bool
  private let runClassifications: Bool
  private let trackingEnabled: Bool

  init(_ options: ImageFaceDetectorOptions) {
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    super.init()
  }

  func detectFaces(image: InputImage) throws -> [any HybridFaceSpec] {
    let uiImage = try createInputImage(uri: try resolveInputImage(image))
    guard let cgImage = uiImage.cgImage else {
      throw RuntimeError.error(withMessage: "Failed to create CGImage from input image!")
    }

    let request = VNDetectFaceLandmarksRequest()
    let handler = VNImageRequestHandler(cgImage: cgImage, orientation: .up, options: [:])
    try handler.perform([request])

    let frameWidth = Double(cgImage.width)
    let frameHeight = Double(cgImage.height)
    let config = createFaceProcessConfig(
      frameWidth,
      frameHeight,
      false,
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled,
      createIdentityPointTransformer()
    )

    return (request.results ?? []).map { HybridFace(face: $0, config: config) }
  }

  private func resolveInputImage(_ input: InputImage) throws -> String {
    switch input {
      case .first(let uri): return uri
      case .third(let object): return object.uri
      case .second: break
    }
    throw RuntimeError.error(withMessage: "Invalid image type. Expected string or { uri }")
  }

  private func createInputImage(uri: String) throws -> UIImage {
    guard let url = URL(string: uri) else {
      throw RuntimeError.error(withMessage: "Invalid image URI")
    }
    if url.isFileURL, let image = UIImage(contentsOfFile: url.path) { return image }
    if !url.isFileURL, let data = try? Data(contentsOf: url), let image = UIImage(data: data) { return image }
    throw RuntimeError.error(withMessage: "Failed to load image from URI")
  }
}
