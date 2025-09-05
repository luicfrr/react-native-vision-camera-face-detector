package com.visioncamerafacedetector

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.facebook.react.bridge.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ImageFaceDetectorModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "ImageFaceDetector"

  @ReactMethod
  fun hasFaceInImage(uriString: String, promise: Promise) {
    Thread {
      try {
        val bitmap = loadBitmapFromUri(uriString)
        if (bitmap == null) {
          promise.reject("E_LOAD_IMAGE", "Could not load image from uri: $uriString")
          return@Thread
        }

        val options = FaceDetectorOptions.Builder()
          .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
          .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
          .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
          .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
          .setMinFaceSize(0.15f)
          .build()

        val detector = FaceDetection.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
          .addOnSuccessListener { faces ->
            promise.resolve(faces.isNotEmpty())
          }
          .addOnFailureListener { e ->
            promise.reject("E_DETECT", "Failed to run face detection", e)
          }
      } catch (e: Exception) {
        promise.reject("E_UNEXPECTED", e.message, e)
      }
    }.start()
  }

  @ReactMethod
  fun countFacesInImage(uriString: String, promise: Promise) {
    Thread {
      try {
        val bitmap = loadBitmapFromUri(uriString)
        if (bitmap == null) {
          promise.reject("E_LOAD_IMAGE", "Could not load image from uri: $uriString")
          return@Thread
        }

        val options = FaceDetectorOptions.Builder()
          .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
          .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
          .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
          .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
          .setMinFaceSize(0.15f)
          .build()

        val detector = FaceDetection.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
          .addOnSuccessListener { faces ->
            promise.resolve(faces.size)
          }
          .addOnFailureListener { e ->
            promise.reject("E_DETECT", "Failed to run face detection", e)
          }
      } catch (e: Exception) {
        promise.reject("E_UNEXPECTED", e.message, e)
      }
    }.start()
  }

  private fun loadBitmapFromUri(uriString: String): Bitmap? {
    return try {
      val uri = Uri.parse(uriString)
      when (uri.scheme?.lowercase()) {
        "content", "android.resource" -> {
          val stream = reactContext.contentResolver.openInputStream(uri)
          stream.useDecode()
        }
        "file" -> {
          val path = uri.path ?: return null
          if (path.startsWith("/android_asset/")) {
            val assetPath = path.removePrefix("/android_asset/")
            reactContext.assets.open(assetPath).useDecode()
          } else {
            BitmapFactory.decodeFile(path)
          }
        }
        "asset" -> {
          val assetPath = uriString.removePrefix("asset:/").removePrefix("asset:///")
          reactContext.assets.open(assetPath).useDecode()
        }
        "http", "https" -> {
          val url = URL(uriString)
          val conn = url.openConnection() as HttpURLConnection
          conn.connect()
          val input = conn.inputStream
          input.useDecode()
        }
        else -> {
          // Fallback try as file path
          BitmapFactory.decodeFile(uriString)
        }
      }
    } catch (e: Exception) {
      null
    }
  }
}

private fun InputStream?.useDecode(): Bitmap? {
  if (this == null) return null
  return try {
    this.use { BitmapFactory.decodeStream(it) }
  } catch (e: Exception) {
    null
  }
}
