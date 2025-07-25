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
import com.mrousavy.camera.frameprocessors.VisionCameraProxy

private const val TAG = "FaceDetector"
class VisionCameraFaceDetectorPlugin(
  proxy: VisionCameraProxy,
  options: Map<String, Any>?
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
  private var orientationManager: VisionCameraFaceDetectorOrientation? = null

  init {
    // initializes orientation manager
    orientationManager = VisionCameraFaceDetectorOrientation(proxy.context)

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

  private fun processBoundingBox(
    boundingBox: Rect,
    sourceWidth: Double,
    sourceHeight: Double,
    scaleX: Double,
    scaleY: Double
  ): Map<String, Any> {
    val bounds: MutableMap<String, Any> = HashMap()
    val width = boundingBox.width().toDouble() * scaleX
    val height = boundingBox.height().toDouble() * scaleY
    val x = boundingBox.left.toDouble()
    val y = boundingBox.top.toDouble()

    bounds["width"] = width
    bounds["height"] = height
    bounds["x"] = x * scaleX
    bounds["y"] = y * scaleY

    if(!autoMode) return bounds

    // using front camera
    if(cameraFacing == Position.FRONT) {
      when (orientationManager?.orientation) {
        // device is portrait
        Surface.ROTATION_0 -> {
          bounds["x"] = ((-x * scaleX) + sourceWidth * scaleX) - width
          bounds["y"] = y * scaleY
        }
        // device is landscape right
        Surface.ROTATION_270 -> {
          bounds["x"] = y * scaleX
          bounds["y"] = x * scaleY
        }
        // device is upside down
        Surface.ROTATION_180 -> {
          bounds["x"] = x * scaleX
          bounds["y"] = ((-y * scaleY) + sourceHeight * scaleY) - height
        }
        // device is landscape left
        Surface.ROTATION_90 -> {
          bounds["x"] = ((-y * scaleX) + sourceWidth * scaleX) - width
          bounds["y"] = ((-x * scaleY) + sourceHeight * scaleY) - height
        }
      }
      return bounds
    }

    // using back camera
    when (orientationManager?.orientation) {
      // device is portrait
      Surface.ROTATION_0 -> {
        bounds["x"] = x * scaleX
        bounds["y"] = y * scaleY
      }
      // device is landscape right
      Surface.ROTATION_270 -> {
        bounds["x"] = y * scaleX
        bounds["y"] = ((-x * scaleY) + sourceHeight * scaleY) - height
      }
      // device is upside down
      Surface.ROTATION_180 -> {
        bounds["x"] =((-x * scaleX) + sourceWidth * scaleX) - width
        bounds["y"] = ((-y * scaleY) + sourceHeight * scaleY) - height
      }
      // device is landscape left
      Surface.ROTATION_90 -> {
        bounds["x"] = ((-y * scaleX) + sourceWidth * scaleX) - width
        bounds["y"] = x * scaleY
      }
    }
    return bounds
  }

  private fun processLandmarks(
    face: Face,
    scaleX: Double,
    scaleY: Double
  ): Map<String, Any> {
    val faceLandmarksTypes = intArrayOf(
      FaceLandmark.LEFT_CHEEK,
      FaceLandmark.LEFT_EAR,
      FaceLandmark.LEFT_EYE,
      FaceLandmark.MOUTH_BOTTOM,
      FaceLandmark.MOUTH_LEFT,
      FaceLandmark.MOUTH_RIGHT,
      FaceLandmark.NOSE_BASE,
      FaceLandmark.RIGHT_CHEEK,
      FaceLandmark.RIGHT_EAR,
      FaceLandmark.RIGHT_EYE
    )
    val faceLandmarksTypesStrings = arrayOf(
      "LEFT_CHEEK",
      "LEFT_EAR",
      "LEFT_EYE",
      "MOUTH_BOTTOM",
      "MOUTH_LEFT",
      "MOUTH_RIGHT",
      "NOSE_BASE",
      "RIGHT_CHEEK",
      "RIGHT_EAR",
      "RIGHT_EYE"
    )
    val faceLandmarksTypesMap: MutableMap<String, Any> = HashMap()
    for (i in faceLandmarksTypesStrings.indices) {
      val landmark = face.getLandmark(faceLandmarksTypes[i])
      val landmarkName = faceLandmarksTypesStrings[i]

      if (landmark == null) continue

      val point = landmark.position
      val currentPointsMap: MutableMap<String, Double> = HashMap()
      currentPointsMap["x"] = point.x.toDouble() * scaleX
      currentPointsMap["y"] = point.y.toDouble() * scaleY
      faceLandmarksTypesMap[landmarkName] = currentPointsMap
    }

    return faceLandmarksTypesMap
  }

  private fun processFaceContours(
    face: Face,
    scaleX: Double,
    scaleY: Double
  ): Map<String, Any> {
    val faceContoursTypes = intArrayOf(
      FaceContour.FACE,
      FaceContour.LEFT_CHEEK,
      FaceContour.LEFT_EYE,
      FaceContour.LEFT_EYEBROW_BOTTOM,
      FaceContour.LEFT_EYEBROW_TOP,
      FaceContour.LOWER_LIP_BOTTOM,
      FaceContour.LOWER_LIP_TOP,
      FaceContour.NOSE_BOTTOM,
      FaceContour.NOSE_BRIDGE,
      FaceContour.RIGHT_CHEEK,
      FaceContour.RIGHT_EYE,
      FaceContour.RIGHT_EYEBROW_BOTTOM,
      FaceContour.RIGHT_EYEBROW_TOP,
      FaceContour.UPPER_LIP_BOTTOM,
      FaceContour.UPPER_LIP_TOP
    )
    val faceContoursTypesStrings = arrayOf(
      "FACE",
      "LEFT_CHEEK",
      "LEFT_EYE",
      "LEFT_EYEBROW_BOTTOM",
      "LEFT_EYEBROW_TOP",
      "LOWER_LIP_BOTTOM",
      "LOWER_LIP_TOP",
      "NOSE_BOTTOM",
      "NOSE_BRIDGE",
      "RIGHT_CHEEK",
      "RIGHT_EYE",
      "RIGHT_EYEBROW_BOTTOM",
      "RIGHT_EYEBROW_TOP",
      "UPPER_LIP_BOTTOM",
      "UPPER_LIP_TOP"
    )
    val faceContoursTypesMap: MutableMap<String, Any> = HashMap()
    for (i in faceContoursTypesStrings.indices) {
      val contour = face.getContour(faceContoursTypes[i])
      val contourName = faceContoursTypesStrings[i]

      if (contour == null) continue

      val points = contour.points
      val pointsMap: MutableList<Map<String, Double>> = mutableListOf()
      for (j in points.indices) {
        val currentPointsMap: MutableMap<String, Double> = HashMap()
        currentPointsMap["x"] = points[j].x.toDouble() * scaleX
        currentPointsMap["y"] = points[j].y.toDouble() * scaleY
        pointsMap.add(currentPointsMap)
      }

      faceContoursTypesMap[contourName] = pointsMap
    }
    return faceContoursTypesMap
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
      Log.e(TAG, "Error processing face detection: ", e)
    } catch (e: FrameInvalidError) {
      Log.e(TAG, "Frame invalid error: ", e)
    }

    return result
  }
}
