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
import com.google.mlkit.vision.common.InputImage
import android.net.Uri

class HybridImageFaceDetector(
  options: ImageFaceDetectorOptions
) : HybridFaceDetectorSpec() {
  private val context = NitroModules.applicationContext
  private val autoMode = options.autoMode ?: false
  private val windowWidth = options.windowWidth ?: 1.0
  private val windowHeight = options.windowHeight ?: 1.0
  private val cameraFacing: Position = options.cameraFacing
  private val faceDetector = FaceDetection.getClient(
    options.toMLFaceDetectorOptions()
  )

  private fun resolveInputImage(
    input: Any?
  ): String {
    return when (input) {
      is String -> input

      is Map<*, *> -> {
        val uri = input["uri"] as? String
        uri ?: throw IllegalArgumentException("Invalid image object: missing 'uri'")
      }

      else -> throw IllegalArgumentException("Invalid image type. Expected string or { uri }")
    }
  }

  private fun createInputImage(uri: String): InputImage {
    val parsedUri = Uri.parse(uri)

    return InputImage.fromFilePath(
      context,
      parsedUri
    )
  }

  override fun detectFaces(
    frame: HybridFrameSpec
  ): Promise<Array<HybridFaceSpec>> {
    val promise = Promise<Array<HybridFaceSpec>>()
    val uri = resolveInputImage(image)
    val image = createInputImage(uri)
    val width = image.height.toDouble()
    val height = image.width.toDouble()
    val config = FaceProcessConfig(
      width = image.height.toDouble(),
      height = image.width.toDouble(),
      scaleX = 1.0,
      scaleY = 1.0,
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
}
