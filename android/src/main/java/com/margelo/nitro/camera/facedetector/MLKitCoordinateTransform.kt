package com.margelo.nitro.camera.facedetector

import android.graphics.Matrix
import android.graphics.RectF

/**
 * Builds the same buffer -> ML Kit rotation matrix used by AndroidX's
 * `MlKitAnalyzer`. ML Kit reports coordinates after the `InputImage` rotation,
 * while CameraX's sensor-to-buffer matrix addresses the unrotated buffer.
 */
internal fun createBufferToMLKitRotationMatrix(
  bufferWidth: Int,
  bufferHeight: Int,
  rotationDegrees: Int
): Matrix {
  val normalizedRotation = normalizeMLKitRotation(rotationDegrees)

  val sourceRect = RectF(0f, 0f, bufferWidth.toFloat(), bufferHeight.toFloat())
  val targetRect =
    if (normalizedRotation == 90 || normalizedRotation == 270) {
      RectF(0f, 0f, bufferHeight.toFloat(), bufferWidth.toFloat())
    } else {
      sourceRect
    }
  val normalizedRect = RectF(-1f, -1f, 1f, 1f)
  val normalizedToTarget = Matrix().apply {
    setRectToRect(normalizedRect, targetRect, Matrix.ScaleToFit.FILL)
  }

  return Matrix().apply {
    setRectToRect(sourceRect, normalizedRect, Matrix.ScaleToFit.FILL)
    postRotate(normalizedRotation.toFloat())
    postConcat(normalizedToTarget)
  }
}

/** Returns the dimensions of the coordinate system used by ML Kit's output. */
internal fun getMLKitCoordinateDimensions(
  bufferWidth: Double,
  bufferHeight: Double,
  rotationDegrees: Int
): Pair<Double, Double> {
  return when (normalizeMLKitRotation(rotationDegrees)) {
    90, 270 -> Pair(bufferHeight, bufferWidth)
    else -> Pair(bufferWidth, bufferHeight)
  }
}

private fun normalizeMLKitRotation(rotationDegrees: Int): Int {
  val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
  require(normalizedRotation % 90 == 0) {
    "Invalid ML Kit rotation: $rotationDegrees"
  }
  return normalizedRotation
}
