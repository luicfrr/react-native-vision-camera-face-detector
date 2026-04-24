package com.nitro.visioncamerafacedetector.extensions

import com.google.mlkit.vision.face.FaceDetectorOptions as MLFaceDetectorOptions
import com.margelo.nitro.camera.facedetector.FaceDetectorOptions

fun FaceDetectorOptions.toMLFaceDetectorOptions(): MLFaceDetectorOptions {
  var performanceModeValue = MLFaceDetectorOptions.PERFORMANCE_MODE_FAST
  var landmarkModeValue = MLFaceDetectorOptions.LANDMARK_MODE_NONE
  var classificationModeValue = MLFaceDetectorOptions.CLASSIFICATION_MODE_NONE
  var contourModeValue = MLFaceDetectorOptions.CONTOUR_MODE_NONE

  if (this.performanceMode.toString() == "accurate") {
    performanceModeValue = MLFaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
  }

  if (this.runLandmarks == true) {
    landmarkModeValue = MLFaceDetectorOptions.LANDMARK_MODE_ALL
  }

  if (this.runContours == true) {
    contourModeValue = MLFaceDetectorOptions.CONTOUR_MODE_ALL
  }

  if (this.runClassifications == true) {
    classificationModeValue = MLFaceDetectorOptions.CLASSIFICATION_MODE_ALL
  }

  val minFaceSize = this.minFaceSize ?: 0.15
  val optionsBuilder = MLFaceDetectorOptions
    .Builder()
    .setPerformanceMode(performanceModeValue)
    .setLandmarkMode(landmarkModeValue)
    .setContourMode(contourModeValue)
    .setClassificationMode(classificationModeValue)
    .setMinFaceSize(minFaceSize.toFloat())

  if (this.trackingEnabled == true) {
    optionsBuilder.enableTracking()
  }

  return optionsBuilder.build()
}
