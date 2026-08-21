package com.margelo.nitro.camera.facedetector

import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.face.FaceDetection
import com.margelo.nitro.camera.HybridFrameSpec
import com.margelo.nitro.camera.Point as CameraPoint
import com.google.android.gms.tasks.Tasks
import com.margelo.nitro.camera.facedetector.extensions.toMLFaceDetectorOptions
import com.margelo.nitro.camera.facedetector.extensions.toInputImage

class HybridFaceDetector(
  options: FaceDetectorOptions
) : HybridFaceDetectorSpec() {
  private val runLandmarks = options.runLandmarks ?: false
  private val runContours = options.runContours ?: false
  private val runClassifications = options.runClassifications ?: false
  private val trackingEnabled = options.trackingEnabled ?: false
  private val autoMode = options.autoMode ?: false
  private val faceDetector = FaceDetection.getClient(
    options.toMLFaceDetectorOptions()
  )

  @OptIn(ExperimentalGetImage::class)
  override fun detectFaces(
    frame: HybridFrameSpec
  ): Array<HybridFaceSpec> {
    val image = frame.toInputImage()
    val (mlKitFrameWidth, mlKitFrameHeight) = getMLKitCoordinateDimensions(
      image.width.toDouble(),
      image.height.toDouble(),
      image.rotationDegrees
    )
    // ML Kit returns points after applying InputImage.rotationDegrees. Undo that
    // rotation before sampling VisionCamera's native raw-frame -> camera transform.
    val pointTransformer = if (autoMode) {
      createMLKitToCameraPointTransformer(
        frame,
        frame.width.toInt(),
        frame.height.toInt(),
        image.rotationDegrees
      )
    } else createIdentityPointTransformer()

    val config = createFaceProcessConfig(
      frameWidth = mlKitFrameWidth,
      frameHeight = mlKitFrameHeight,
      autoMode = autoMode,
      runLandmarks = runLandmarks,
      runContours = runContours,
      runClassifications = runClassifications,
      trackingEnabled = trackingEnabled,
      pointTransformer = pointTransformer
    )
    val task = faceDetector.process(image)
    val faces = Tasks.await(task).map {
      HybridFace(it, config)
    }.toTypedArray<HybridFaceSpec>()

    return faces
  }

  override fun dispose() {
    faceDetector.close()
    super.dispose()
  }

  private fun createMLKitToCameraPointTransformer(
    frame: HybridFrameSpec,
    frameWidth: Int,
    frameHeight: Int,
    rotationDegrees: Int
  ): (Double, Double) -> Pair<Double, Double> {
    val mlKitToFrame = Matrix()
    check(
      createBufferToMLKitRotationMatrix(
        frameWidth,
        frameHeight,
        rotationDegrees
      ).invert(mlKitToFrame)
    ) {
      "Could not invert ML Kit buffer rotation matrix."
    }

    fun convertPoint(x: Double, y: Double): CameraPoint {
      val point = floatArrayOf(x.toFloat(), y.toFloat())
      mlKitToFrame.mapPoints(point)
      return frame.convertFramePointToCameraPoint(
        CameraPoint(point[0].toDouble(), point[1].toDouble())
      )
    }

    // Capture the composed affine transform while the Frame is still valid.
    val origin = convertPoint(0.0, 0.0)
    val xAxis = convertPoint(1.0, 0.0)
    val yAxis = convertPoint(0.0, 1.0)

    return { 
      x, y -> Pair(
        origin.x + (xAxis.x - origin.x) * x + (yAxis.x - origin.x) * y,
        origin.y + (xAxis.y - origin.y) * x + (yAxis.y - origin.y) * y
      )
    }
  }
}
