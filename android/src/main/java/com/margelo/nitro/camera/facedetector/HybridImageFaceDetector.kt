package com.margelo.nitro.camera.facedetector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.face.FaceDetection
import com.margelo.nitro.camera.facedetector.InputImage
import com.nitro.visioncamerafacedetector.extensions.toMLFaceDetectorOptions
import com.margelo.nitro.core.Promise
import com.google.mlkit.vision.common.InputImage as MLInputImage
import com.margelo.nitro.NitroModules
import com.margelo.nitro.camera.facedetector.FaceDetectorOptions
import com.margelo.nitro.camera.facedetector.HybridImageFaceDetectorSpec
import com.margelo.nitro.camera.facedetector.HybridFaceSpec
import androidx.core.net.toUri
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class HybridImageFaceDetector(
  options: FaceDetectorOptions
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
  ): Promise<Array<HybridFaceSpec>>  {
    val promise = Promise<Array<HybridFaceSpec>>()
    val uri = resolveInputImage(image)
    val mlImage = createInputImage(uri)
    val config = FaceProcessConfig(
      width = mlImage.height.toDouble(),
      height = mlImage.width.toDouble(),
      scaleX = 1.0,
      scaleY = 1.0,
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled
    )

    faceDetector
      .process(mlImage)
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
