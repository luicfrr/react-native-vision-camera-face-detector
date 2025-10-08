package com.visioncamerafacedetector

import android.util.Log
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

private const val TAG = "ImageFaceDetector"
class ImageFaceDetectorModule(
  private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "ImageFaceDetector"

  @ReactMethod
  fun detectFaces(
    uri: String, 
    options: ReadableMap?,
    promise: Promise
  ) {
    val common = FaceDetectorCommon()
    val result: WritableArray = Arguments.createArray()

    try {
      var performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_FAST
      var landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_NONE
      var classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_NONE
      var contourModeValue = FaceDetectorOptions.CONTOUR_MODE_NONE
      var runLandmarks = false
      var runClassifications = false
      var runContours = false
      var trackingEnabled = false

      if (options?.getString("performanceMode") == "accurate") {
        performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
      }

      if (options?.getString("landmarkMode") == "all") {
        runLandmarks = true
        landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_ALL
      }

      if (options?.getString("classificationMode") == "all") {
        runClassifications = true
        classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_ALL
      }

      if (options?.getString("contourMode") == "all") {
        runContours = true
        contourModeValue = FaceDetectorOptions.CONTOUR_MODE_ALL
      }

      val minFaceSize: Double = (options?.getDouble("minFaceSize") ?: 0.15) as Double
      val optionsBuilder = FaceDetectorOptions.Builder()
        .setPerformanceMode(performanceModeValue)
        .setLandmarkMode(landmarkModeValue)
        .setContourMode(contourModeValue)
        .setClassificationMode(classificationModeValue)
        .setMinFaceSize(minFaceSize.toFloat())

      if (options?.getString("trackingEnabled") == "true") {
        trackingEnabled = true
        optionsBuilder.enableTracking()
      }

      val faceDetector = FaceDetection.getClient(
        optionsBuilder.build()
      )
      val image = InputImage.fromBitmap(
        loadBitmapFromUri(uri)!!, 0
      )
      faceDetector.process(image)
        .addOnSuccessListener{faces ->
          faces.forEach{face ->
            val map: WritableMap = Arguments.createMap()

            if (runLandmarks) {
              map.putMap("landmarks", common.processLandmarks(face))
            }

            if (runClassifications) {
              map.putDouble("leftEyeOpenProbability", face.leftEyeOpenProbability?.toDouble() ?: -1.0)
              map.putDouble("rightEyeOpenProbability", face.rightEyeOpenProbability?.toDouble() ?: -1.0)
              map.putDouble("smilingProbability", face.smilingProbability?.toDouble() ?: -1.0)
            }

            if (runContours) {
              map.putMap("contours", common.processFaceContours(face))
            }

            if (trackingEnabled) {
              map.putInt("trackingId", face.trackingId ?: -1)
            }

            map.putDouble("rollAngle", face.headEulerAngleZ.toDouble())
            map.putDouble("pitchAngle", face.headEulerAngleX.toDouble())
            map.putDouble("yawAngle", face.headEulerAngleY.toDouble())
            map.putMap("bounds", common.processBoundingBox(face.boundingBox))

            result.pushMap(map)
          }

          promise.resolve(result)
        }
        .addOnFailureListener { e ->
          Log.e(TAG, "Error processing image face detection: ", e)
          promise.reject("E_DETECT", "Error processing image face detection", e)
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error preparing face detection: ", e)
      promise.reject("E_LOAD", "Error preparing face detection: ", e)
    }
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
    this.use { 
      BitmapFactory.decodeStream(it)
    }
  } catch (e: Exception) {
    null
  }
}
