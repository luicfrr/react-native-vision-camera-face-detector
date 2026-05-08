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
) -> FaceDetectorOptions {
  let options = FaceDetectorOptions()

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

  options.minFaceSize = NSNumber(
    value: minFaceSize ?? 0.15
  ).floatValue

  if trackingEnabled == true {
    options.isTrackingEnabled = true
  }

  return options
}
