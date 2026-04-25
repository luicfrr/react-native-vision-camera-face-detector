package com.margelo.nitro.camera.facedetector

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.face.FaceDetection
import com.margelo.nitro.NitroModules
import com.margelo.nitro.camera.HybridFrameSpec
import com.margelo.nitro.core.Promise
import com.margelo.nitro.camera.facedetector.extensions.toMLFaceDetectorOptions
import com.margelo.nitro.camera.facedetector.extensions.toInputImage

class HybridFaceDetector(
  options: FaceDetectorOptions
) : HybridFaceDetectorSpec() {
  private val context = NitroModules.applicationContext ?: throw Error("Face Detector - No Context available!")
  private val orientationManager = FaceDetectorOrientation.get(context.applicationContext)
  private val runLandmarks = options.runLandmarks ?: false
  private val runContours = options.runContours ?: false
  private val runClassifications = options.runClassifications ?: false
  private val trackingEnabled = options.trackingEnabled ?: false
  private val autoMode = options.autoMode ?: false
  private val cameraFacing: CameraPosition = options.cameraFacing ?: CameraPosition.FRONT
  private val windowWidth = options.windowWidth ?: 1.0
  private val windowHeight = options.windowHeight ?: 1.0
  private val faceDetector = FaceDetection.getClient(
    options.toMLFaceDetectorOptions()
  )

  @OptIn(ExperimentalGetImage::class)
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
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled,
      autoMode,
      cameraFacing,
      orientation = orientationManager.orientation
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
