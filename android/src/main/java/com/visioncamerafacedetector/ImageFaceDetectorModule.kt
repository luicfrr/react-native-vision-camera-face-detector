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

private const val TAG = "ImageFaceDetector"
class ImageFaceDetectorModule(
  private val reactContext: ReactApplicationContext
) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "ImageFaceDetector"

  @ReactMethod
  fun detectFaces(
    uri: String, 
    options: Map<String, Any>?
  ) {
    val result = ArrayList<Map<String, Any>>()

    try {
      var performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_FAST
      var landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_NONE
      var classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_NONE
      var contourModeValue = FaceDetectorOptions.CONTOUR_MODE_NONE
      var runLandmarks = false
      var runClassifications = false
      var runContours = false
      var trackingEnabled = false

      val optionsBuilder = FaceDetectorOptions.Builder()
        .setPerformanceMode(performanceModeValue)
        .setLandmarkMode(landmarkModeValue)
        .setContourMode(contourModeValue)
        .setClassificationMode(classificationModeValue)
        .setMinFaceSize(minFaceSize.toFloat())

      if (options?.get("trackingEnabled").toString() == "true") {
        trackingEnabled = true
        optionsBuilder.enableTracking()
      }

      val faceDetector = FaceDetection.getClient(
        optionsBuilder.build()
      )
      val image = InputImage.fromBitmap(
        loadBitmapFromUri(uri), 0
      )
      val task = faceDetector.process(image)
      val faces = Tasks.await(task)
      
      faces.forEach{face ->
        val map: MutableMap<String, Any> = HashMap()

        if (runLandmarks) {
          map["landmarks"] = processLandmarks(
            face,
            scaleX,
            scaleY
          )
        }

        if (runClassifications) {
          map["leftEyeOpenProbability"] = face.leftEyeOpenProbability?.toDouble() ?: -1
          map["rightEyeOpenProbability"] = face.rightEyeOpenProbability?.toDouble() ?: -1
          map["smilingProbability"] = face.smilingProbability?.toDouble() ?: -1
        }

        if (runContours) {
          map["contours"] = processFaceContours(
            face,
            scaleX,
            scaleY
          )
        }

        if (trackingEnabled) {
          map["trackingId"] = face.trackingId ?: -1
        }

        map["rollAngle"] = face.headEulerAngleZ.toDouble()
        map["pitchAngle"] = face.headEulerAngleX.toDouble()
        map["yawAngle"] = face.headEulerAngleY.toDouble()
        map["bounds"] = processBoundingBox(
          face.boundingBox,
          width,
          height,
          scaleX,
          scaleY
        )
        result.add(map)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error processing image face detection: ", e)
    }

    return result
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
