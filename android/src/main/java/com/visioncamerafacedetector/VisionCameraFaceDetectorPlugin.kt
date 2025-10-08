package com.visioncamerafacedetector

import android.graphics.Rect
import android.util.Log
import android.view.Surface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.mrousavy.camera.core.FrameInvalidError
import com.mrousavy.camera.core.types.Position
import com.mrousavy.camera.frameprocessors.Frame
import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin

private const val TAG = "FaceDetector"
class VisionCameraFaceDetectorPlugin(
  options: Map<String, Any>?,
  private val orientationManager: VisionCameraFaceDetectorOrientation
) : FrameProcessorPlugin() {
  // detection props
  private var autoMode = false
  private var faceDetector: FaceDetector? = null
  private var runLandmarks = false
  private var runClassifications = false
  private var runContours = false
  private var trackingEnabled = false
  private var windowWidth = 1.0
  private var windowHeight = 1.0
  private var cameraFacing: Position = Position.FRONT
  private val common = FaceDetectorCommon()

  init {
    // handle auto scaling
    autoMode = options?.get("autoMode").toString() == "true"

    // initializes faceDetector on creation
    var performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_FAST
    var landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_NONE
    var classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_NONE
    var contourModeValue = FaceDetectorOptions.CONTOUR_MODE_NONE

    windowWidth = (options?.get("windowWidth") ?: 1.0) as Double
    windowHeight = (options?.get("windowHeight") ?: 1.0) as Double

    if (options?.get("cameraFacing").toString() == "back") {
      cameraFacing = Position.BACK
    }

    if (options?.get("performanceMode").toString() == "accurate") {
      performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
    }

    if (options?.get("landmarkMode").toString() == "all") {
      runLandmarks = true
      landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_ALL
    }

    if (options?.get("classificationMode").toString() == "all") {
      runClassifications = true
      classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_ALL
    }

    if (options?.get("contourMode").toString() == "all") {
      runContours = true
      contourModeValue = FaceDetectorOptions.CONTOUR_MODE_ALL
    }

    val minFaceSize: Double = (options?.get("minFaceSize") ?: 0.15) as Double
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

    faceDetector = FaceDetection.getClient(
      optionsBuilder.build()
    )
  }

  private fun getImageOrientation(): Int {
    return when (orientationManager?.orientation) {
      // device is portrait
      Surface.ROTATION_0 -> if(cameraFacing == Position.FRONT) 270 else 90
      // device is landscape right
      Surface.ROTATION_270 -> if(cameraFacing == Position.FRONT) 180 else 180
      // device is upside down
      Surface.ROTATION_180 -> if(cameraFacing == Position.FRONT) 90 else 270
      // device is landscape left
      Surface.ROTATION_90 -> if(cameraFacing == Position.FRONT) 0 else 0
      else -> 0
    }
  }

  override fun callback(
    frame: Frame,
    params: Map<String, Any>?
  ): Any {
    val result = ArrayList<Map<String, Any>>()
    
    try {
      val image = InputImage.fromMediaImage(frame.image, getImageOrientation())
      // we need to invert sizes as frame is always -90deg rotated
      val width = image.height.toDouble()
      val height = image.width.toDouble()
      val scaleX = if(autoMode) windowWidth / width else 1.0
      val scaleY = if(autoMode) windowHeight / height else 1.0
      val task = faceDetector!!.process(image)
      val faces = Tasks.await(task)
      faces.forEach{face ->
        val map: MutableMap<String, Any> = HashMap()

        if (runLandmarks) {
          map["landmarks"] = common.processLandmarks(
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
          map["contours"] = common.processFaceContours(
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
        map["bounds"] = common.processBoundingBox(
          face.boundingBox,
          width,
          height,
          scaleX,
          scaleY
        )
        result.add(map)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error processing face detection: ", e)
    } catch (e: FrameInvalidError) {
      Log.e(TAG, "Frame invalid error: ", e)
    }

    return result
  }
}
