package com.visioncamerafacedetector

import android.graphics.Rect
import android.view.Surface
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.mrousavy.camera.core.types.Position
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceContour

class FaceDetectorCommon() {
  fun processBoundingBox(
    boundingBox: Rect,
    sourceWidth: Double = 0.0,
    sourceHeight: Double = 0.0,
    scaleX: Double = 1.0,
    scaleY: Double = 1.0,
    autoMode: Boolean = false,
    cameraFacing: Position = Position.FRONT,
    orientationManager: VisionCameraFaceDetectorOrientation? = null
  ): WritableMap {
    val bounds: WritableMap = Arguments.createMap()
    val width = boundingBox.width().toDouble() * scaleX
    val height = boundingBox.height().toDouble() * scaleY
    val x = boundingBox.left.toDouble()
    val y = boundingBox.top.toDouble()

    bounds.putDouble("width", width)
    bounds.putDouble("height", height)
    bounds.putDouble("x", x * scaleX)
    bounds.putDouble("y", y * scaleY)

    if(!autoMode) return bounds

    // using front camera
    if(cameraFacing == Position.FRONT) {
      when (orientationManager?.orientation) {
        // device is portrait
        Surface.ROTATION_0 -> {
          bounds.putDouble("x", ((-x * scaleX) + sourceWidth * scaleX) - width)
          bounds.putDouble("y", y * scaleY)
        }
        // device is landscape right
        Surface.ROTATION_270 -> {
          bounds.putDouble("x", y * scaleX)
          bounds.putDouble("y", x * scaleY)
        }
        // device is upside down
        Surface.ROTATION_180 -> {
          bounds.putDouble("x", x * scaleX)
          bounds.putDouble("y", ((-y * scaleY) + sourceHeight * scaleY) - height)
        }
        // device is landscape left
        Surface.ROTATION_90 -> {
          bounds.putDouble("x", ((-y * scaleX) + sourceWidth * scaleX) - width)
          bounds.putDouble("y", ((-x * scaleY) + sourceHeight * scaleY) - height)
        }
      }
      return bounds
    }

    // using back camera
    when (orientationManager?.orientation) {
      // device is portrait
      Surface.ROTATION_0 -> {
        bounds.putDouble("x", x * scaleX)
        bounds.putDouble("y", y * scaleY)
      }
      // device is landscape right
      Surface.ROTATION_270 -> {
        bounds.putDouble("x", y * scaleX)
        bounds.putDouble("y", ((-x * scaleY) + sourceHeight * scaleY) - height)
      }
      // device is upside down
      Surface.ROTATION_180 -> {
        bounds.putDouble("x",((-x * scaleX) + sourceWidth * scaleX) - width)
        bounds.putDouble("y", ((-y * scaleY) + sourceHeight * scaleY) - height)
      }
      // device is landscape left
      Surface.ROTATION_90 -> {
        bounds.putDouble("x", ((-y * scaleX) + sourceWidth * scaleX) - width)
        bounds.putDouble("y", x * scaleY)
      }
    }
    return bounds
  }

  fun processLandmarks(
    face: Face,
    scaleX: Double = 1.0,
    scaleY: Double = 1.0
  ): WritableMap {
    val faceLandmarksTypes = intArrayOf(
      FaceLandmark.LEFT_CHEEK,
      FaceLandmark.LEFT_EAR,
      FaceLandmark.LEFT_EYE,
      FaceLandmark.MOUTH_BOTTOM,
      FaceLandmark.MOUTH_LEFT,
      FaceLandmark.MOUTH_RIGHT,
      FaceLandmark.NOSE_BASE,
      FaceLandmark.RIGHT_CHEEK,
      FaceLandmark.RIGHT_EAR,
      FaceLandmark.RIGHT_EYE
    )
    val faceLandmarksTypesStrings = arrayOf(
      "LEFT_CHEEK",
      "LEFT_EAR",
      "LEFT_EYE",
      "MOUTH_BOTTOM",
      "MOUTH_LEFT",
      "MOUTH_RIGHT",
      "NOSE_BASE",
      "RIGHT_CHEEK",
      "RIGHT_EAR",
      "RIGHT_EYE"
    )
    val faceLandmarksTypesMap: WritableMap = Arguments.createMap()
    for (i in faceLandmarksTypesStrings.indices) {
      val landmark = face.getLandmark(faceLandmarksTypes[i])
      val landmarkName = faceLandmarksTypesStrings[i]

      if (landmark == null) continue

      val point = landmark.position
      val currentPointsMap: WritableMap = Arguments.createMap()
      currentPointsMap.putDouble("x", point.x.toDouble() * scaleX)
      currentPointsMap.putDouble("y", point.y.toDouble() * scaleY)
      faceLandmarksTypesMap.putMap(landmarkName, currentPointsMap)
    }

    return faceLandmarksTypesMap
  }

  fun processFaceContours(
    face: Face,
    scaleX: Double = 1.0,
    scaleY: Double = 1.0
  ): WritableMap {
    val faceContoursTypes = intArrayOf(
      FaceContour.FACE,
      FaceContour.LEFT_CHEEK,
      FaceContour.LEFT_EYE,
      FaceContour.LEFT_EYEBROW_BOTTOM,
      FaceContour.LEFT_EYEBROW_TOP,
      FaceContour.LOWER_LIP_BOTTOM,
      FaceContour.LOWER_LIP_TOP,
      FaceContour.NOSE_BOTTOM,
      FaceContour.NOSE_BRIDGE,
      FaceContour.RIGHT_CHEEK,
      FaceContour.RIGHT_EYE,
      FaceContour.RIGHT_EYEBROW_BOTTOM,
      FaceContour.RIGHT_EYEBROW_TOP,
      FaceContour.UPPER_LIP_BOTTOM,
      FaceContour.UPPER_LIP_TOP
    )
    val faceContoursTypesStrings = arrayOf(
      "FACE",
      "LEFT_CHEEK",
      "LEFT_EYE",
      "LEFT_EYEBROW_BOTTOM",
      "LEFT_EYEBROW_TOP",
      "LOWER_LIP_BOTTOM",
      "LOWER_LIP_TOP",
      "NOSE_BOTTOM",
      "NOSE_BRIDGE",
      "RIGHT_CHEEK",
      "RIGHT_EYE",
      "RIGHT_EYEBROW_BOTTOM",
      "RIGHT_EYEBROW_TOP",
      "UPPER_LIP_BOTTOM",
      "UPPER_LIP_TOP"
    )
    val faceContoursTypesMap: WritableMap = Arguments.createMap()
    for (i in faceContoursTypesStrings.indices) {
      val contour = face.getContour(faceContoursTypes[i])
      val contourName = faceContoursTypesStrings[i]

      if (contour == null) continue

      val points = contour.points
      val pointsMap: WritableArray = Arguments.createArray()
      for (j in points.indices) {
        val currentPointsMap: WritableMap = Arguments.createMap()
        currentPointsMap.putDouble("x", points[j].x.toDouble() * scaleX)
        currentPointsMap.putDouble("y", points[j].y.toDouble() * scaleY)
        pointsMap.pushMap(currentPointsMap)
      }

      faceContoursTypesMap.putArray(contourName, pointsMap)
    }
    return faceContoursTypesMap
  }
}
