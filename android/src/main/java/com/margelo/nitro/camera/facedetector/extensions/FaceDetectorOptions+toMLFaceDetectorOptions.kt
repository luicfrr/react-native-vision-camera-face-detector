package com.margelo.nitro.camera.facedetector.extensions

import com.google.mlkit.vision.face.FaceDetectorOptions as MLFaceDetectorOptions
import com.margelo.nitro.camera.facedetector.FaceDetectorOptions
import com.margelo.nitro.camera.facedetector.ImageFaceDetectorOptions
import com.margelo.nitro.camera.facedetector.FaceDetectorOutputOptions

fun FaceDetectorOptions.toMLFaceDetectorOptions(): MLFaceDetectorOptions {
  return buildMLFaceDetectorOptions(
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled
  )
}

fun ImageFaceDetectorOptions.toMLFaceDetectorOptions(): MLFaceDetectorOptions {
  return buildMLFaceDetectorOptions(
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled
  )
}

fun FaceDetectorOutputOptions.toMLFaceDetectorOptions(): MLFaceDetectorOptions {
  return buildMLFaceDetectorOptions(
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled
  )
}

private fun buildMLFaceDetectorOptions(
  performanceMode: Any?,
  runLandmarks: Boolean?,
  runContours: Boolean?,
  runClassifications: Boolean?,
  minFaceSize: Double?,
  trackingEnabled: Boolean?
): MLFaceDetectorOptions {
  var performanceModeValue = MLFaceDetectorOptions.PERFORMANCE_MODE_FAST
  var landmarkModeValue = MLFaceDetectorOptions.LANDMARK_MODE_NONE
  var classificationModeValue = MLFaceDetectorOptions.CLASSIFICATION_MODE_NONE
  var contourModeValue = MLFaceDetectorOptions.CONTOUR_MODE_NONE

  if (performanceMode.toString() == "accurate") {
    performanceModeValue = MLFaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
  }

  if (runLandmarks == true) {
    landmarkModeValue = MLFaceDetectorOptions.LANDMARK_MODE_ALL
  }

  if (runContours == true) {
    contourModeValue = MLFaceDetectorOptions.CONTOUR_MODE_ALL
  }

  if (runClassifications == true) {
    classificationModeValue = MLFaceDetectorOptions.CLASSIFICATION_MODE_ALL
  }

  val optionsBuilder = MLFaceDetectorOptions
    .Builder()
    .setPerformanceMode(performanceModeValue)
    .setLandmarkMode(landmarkModeValue)
    .setContourMode(contourModeValue)
    .setClassificationMode(classificationModeValue)
    .setMinFaceSize((minFaceSize ?: 0.15).toFloat())

  if (trackingEnabled == true) {
    optionsBuilder.enableTracking()
  }

  return optionsBuilder.build()
}
