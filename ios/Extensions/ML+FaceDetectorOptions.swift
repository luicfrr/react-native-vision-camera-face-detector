import MLKitFaceDetection

extension FaceDetectorOptions {
  func toMLFaceDetectorOptions() -> MLKitFaceDetection.FaceDetectorOptions {
    buildMLFaceDetectorOptions(
      performanceMode: performanceMode,
      runLandmarks: runLandmarks,
      runContours: runContours,
      runClassifications: runClassifications,
      minFaceSize: minFaceSize,
      trackingEnabled: trackingEnabled
    )
  }
}

extension ImageFaceDetectorOptions {
  func toMLFaceDetectorOptions() -> MLKitFaceDetection.FaceDetectorOptions {
    buildMLFaceDetectorOptions(
      performanceMode: performanceMode,
      runLandmarks: runLandmarks,
      runContours: runContours,
      runClassifications: runClassifications,
      minFaceSize: minFaceSize,
      trackingEnabled: trackingEnabled
    )
  }
}

extension FaceDetectorOutputOptions {
  func toMLFaceDetectorOptions() -> MLKitFaceDetection.FaceDetectorOptions {
    buildMLFaceDetectorOptions(
      performanceMode: performanceMode,
      runLandmarks: runLandmarks,
      runContours: runContours,
      runClassifications: runClassifications,
      minFaceSize: minFaceSize,
      trackingEnabled: trackingEnabled
    )
  }
}

private func buildMLFaceDetectorOptions(
    performanceMode: Any?,
    runLandmarks: Bool?,
    runContours: Bool?,
    runClassifications: Bool?,
    minFaceSize: Double?,
    trackingEnabled: Bool?
) -> MLKitFaceDetection.FaceDetectorOptions {
  let options = MLKitFaceDetection.FaceDetectorOptions()

  options.performanceMode = (
    String(describing: performanceMode) == "accurate"
  ) ? .accurate : .fast

  options.landmarkMode = (
    runLandmarks == true
  ) ? .all : .none

  options.contourMode = (
    runContours == true
  ) ? .all : .none

  options.classificationMode = (
    runClassifications == true
  ) ? .all : .none

  options.minFaceSize = CGFloat(
    minFaceSize ?? 0.15
  )

  if trackingEnabled == true {
    options.isTrackingEnabled = true
  }

  return options
}
