package com.margelo.nitro.camera.facedetector

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.face.FaceDetection
import com.margelo.nitro.camera.facedetector.extensions.toMLFaceDetectorOptions
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage as MLInputImage
import com.margelo.nitro.NitroModules
import androidx.core.net.toUri

class HybridImageFaceDetector(
  options: ImageFaceDetectorOptions
) : HybridImageFaceDetectorSpec() {
  private val context = NitroModules.applicationContext ?: throw Error("Image Face Detector - No Context available!")
  private val runLandmarks = options.runLandmarks ?: false
  private val runContours = options.runContours ?: false
  private val runClassifications = options.runClassifications ?: false
  private val trackingEnabled = options.trackingEnabled ?: false
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

  private fun createInputImage(uri: String): MLInputImage {
    val parsedUri = uri.toUri()

    return MLInputImage.fromFilePath(
      context,
      parsedUri
    )
  }

  @OptIn(ExperimentalGetImage::class)
  override fun detectFaces(
    image: InputImage
  ): Array<HybridFaceSpec> {
    val uri = resolveInputImage(image)
    val mlImage = createInputImage(uri)
    val config = createFaceProcessConfig(
      frameWidth = mlImage.width.toDouble(),
      frameHeight = mlImage.height.toDouble(),
      autoMode = false,
      pointTransformer = createIdentityPointTransformer(),
      runLandmarks = runLandmarks,
      runContours = runContours,
      runClassifications = runClassifications,
      trackingEnabled = trackingEnabled
    )
    val task = faceDetector.process(mlImage)
    val faces = Tasks.await(task).map {
      HybridFace(it, config)
    }.toTypedArray<HybridFaceSpec>()

    return faces
  }
}
