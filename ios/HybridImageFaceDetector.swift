import Foundation
import MLKitFaceDetection
import MLKitVision

class HybridImageFaceDetector: HybridImageFaceDetectorSpec {
  private let runLandmarks: Bool
  private let runContours: Bool
  private let runClassifications: Bool
  private let trackingEnabled: Bool
  private let faceDetector: FaceDetector

  init(_ options: ImageFaceDetectorOptions) {
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    self.faceDetector = FaceDetector.faceDetector(
      options: options.toMLFaceDetectorOptions()
    )

    super.init()
  }

  func detectFaces(
    image: InputImage
  ) throws -> [any HybridFaceSpec] {
    let uiImage = try createInputImage(
      uri: try resolveInputImage(image)
    )
    let mlImage = VisionImage(image: uiImage)
    // ML Kit face coordinates are expressed in image pixels, not UIKit points.
    let frameWidth = Double(uiImage.cgImage?.width ?? Int(uiImage.size.width * uiImage.scale))
    let frameHeight = Double(uiImage.cgImage?.height ?? Int(uiImage.size.height * uiImage.scale))

    let config = createFaceProcessConfig(
      frameWidth,
      frameHeight,
      false,
      createIdentityPointTransformer(),
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled
    )

    let faces = try faceDetector.results(in: mlImage)
    return faces.map {
      HybridFace(
        face: $0,
        config: config
      )
    }
  }

  private func resolveInputImage(
    _ input: Any
  ) throws -> String {
    if let uri = input as? String {
      return uri
    }

    if let object = input as? [String: Any],
      let uri = object["uri"] as? String {
      return uri
    }

    throw NSError(
      domain: "HybridImageFaceDetector",
      code: -1,
      userInfo: [
        NSLocalizedDescriptionKey:
        "Invalid image type. Expected string or { uri }"
      ]
    )
  }

  private func createInputImage(
    uri: String
  ) throws -> UIImage {
    guard let url = URL(string: uri) else {
      throw NSError(
        domain: "HybridImageFaceDetector",
        code: -2,
        userInfo: [
          NSLocalizedDescriptionKey:
          "Invalid image URI"
        ]
      )
    }

    let image: UIImage
    if url.isFileURL {
      guard let loadedImage = UIImage(contentsOfFile: url.path)
      else {
        throw NSError(
          domain: "HybridImageFaceDetector",
          code: -3,
          userInfo: [
            NSLocalizedDescriptionKey:
            "Failed to load image from file"
          ]
        )
      }

      image = loadedImage
    } else {
      guard let data = try? Data(contentsOf: url),
            let loadedImage = UIImage(data: data)
      else {
        throw NSError(
          domain: "HybridImageFaceDetector",
          code: -4,
          userInfo: [
            NSLocalizedDescriptionKey:
            "Failed to load image from URL"
          ]
        )
      }

      image = loadedImage
    }

    return image
  }
}
