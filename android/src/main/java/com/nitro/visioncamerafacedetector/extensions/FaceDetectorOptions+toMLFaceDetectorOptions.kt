package com.nitro.visioncamerafacedetector.extensions

import com.google.mlkit.vision.face.FaceDetectorOptions

fun FaceDetectorOptions.toMLFaceDetectorOptions(): FaceDetectorOptions {
  var performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_FAST
  var landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_NONE
  var classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_NONE
  var contourModeValue = FaceDetectorOptions.CONTOUR_MODE_NONE

  if (this.performanceMode.toString() == "accurate") {
    performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
  }

  if (this.runLandmarks) {
    landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_ALL
  }

  if (this.runContours) {
    contourModeValue = FaceDetectorOptions.CONTOUR_MODE_ALL
  }

  if (this.runClassifications) {
    classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_ALL
  }

  val minFaceSize = (this.minFaceSize ?: 0.15) as Double
  val optionsBuilder = FaceDetectorOptions
    .Builder()
    .setPerformanceMode(performanceModeValue)
    .setLandmarkMode(landmarkModeValue)
    .setContourMode(contourModeValue)
    .setClassificationMode(classificationModeValue)
    .setMinFaceSize(minFaceSize.toFloat())

  if (this.trackingEnabled.toString() == "true") {
    optionsBuilder.enableTracking()
  }

  return optionsBuilder.build()
}
