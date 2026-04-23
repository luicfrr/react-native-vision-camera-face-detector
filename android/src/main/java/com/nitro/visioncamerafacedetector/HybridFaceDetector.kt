package com.nitro.visioncamerafacedetector

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetection
import com.margelo.nitro.camera.HybridFrameSpec
import com.nitro.visioncamerafacedetector.extensions.toInputImage
import com.nitro.visioncamerafacedetector.extensions.toMLFaceDetectorOptions
import com.margelo.nitro.core.Promise

class HybridFaceDetector(
  options: FaceDetectorOptions
) : HybridFaceDetectorSpec() {
  private val context = NitroModules.applicationContext
  private val orientationManager = FaceDetectorOrientation.get(context)
  private val autoMode = options.autoMode ?: false
  private val windowWidth = options.windowWidth ?: 1.0
  private val windowHeight = options.windowHeight ?: 1.0
  private val cameraFacing: Position = options.cameraFacing
  private val faceDetector = FaceDetection.getClient(
    options.toMLFaceDetectorOptions()
  )

  override fun detectFaces(
    frame: HybridFrameSpec
  ): Promise<Array<HybridFaceSpec>> {
    val promise = Promise<Array<HybridFaceSpec>>()
    val image = frame.toInputImage()
    val width = image.height.toDouble()
    val height = image.width.toDouble()
    val scaleX = if(autoMode) windowWidth / width else 1.0
    val scaleY = if(autoMode) windowHeight / height else 1.0
    val config = FaceProcessConfig(
      width,
      height,
      scaleX,
      scaleY,
      autoMode,
      cameraFacing,
      orientation = orientationManager.orientation,
      runLandmarks = config.runLandmarks,
      runContours = config.runContours,
      runClassifications = config.runClassifications,
      trackingEnabled = config.trackingEnabled
    )

    faceDetector
      .process(image)
      .addOnSuccessListener { faces ->
        val hybridFaces = faces.map { 
          HybridFace(it, config)
        }.toTypedArray<HybridFaceSpec>()

        promise.resolve(hybridFaces)
      }.addOnFailureListener { error ->
        promise.reject(error)
      }

    return promise
  }

  override fun stopListeners() {
    FaceDetectorOrientation.stop()
  }
}
