package com.visioncamerafacedetector

import android.graphics.Rect
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.mrousavy.camera.core.FrameInvalidError
import com.mrousavy.camera.core.types.Orientation
import com.mrousavy.camera.frameprocessors.Frame
import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin
import com.mrousavy.camera.frameprocessors.VisionCameraProxy

private const val TAG = "FaceDetector"
class VisionCameraFaceDetectorPlugin(
  proxy: VisionCameraProxy,
  options: Map<String, Any>?
) : FrameProcessorPlugin() {
  // device display data
  private val displayMetrics = proxy.context.resources.displayMetrics
  private val density = displayMetrics.density
  private val windowWidth = (displayMetrics.widthPixels).toDouble() / density
  private val windowHeight = (displayMetrics.heightPixels).toDouble() / density

  // detection props
  private var autoScale = false
  private var faceDetector: FaceDetector? = null
  private var runLandmarks = false
  private var runClassifications = false
  private var runContours = false
  private var trackingEnabled = false

  init {
    // handle auto scaling
    autoScale = options?.get("autoScale").toString() == "true"

    // initializes faceDetector on creation
    var performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_FAST
    var landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_NONE
    var classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_NONE
    var contourModeValue = FaceDetectorOptions.CONTOUR_MODE_NONE
    var minFaceSize = 0.15f

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

    val minFaceSizeParam = options?.get("minFaceSize").toString()
    if (
      minFaceSizeParam != "null" &&
      minFaceSizeParam != minFaceSize.toString()
    ) {
      minFaceSize = minFaceSizeParam.toFloat()
    }

    val optionsBuilder = FaceDetectorOptions.Builder()
      .setPerformanceMode(performanceModeValue)
      .setLandmarkMode(landmarkModeValue)
      .setContourMode(contourModeValue)
      .setClassificationMode(classificationModeValue)
      .setMinFaceSize(minFaceSize)

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
    orientation: Orientation,
    scaleX: Double,
    scaleY: Double
  ): Map<String, Any> {
    val bounds: MutableMap<String, Any> = HashMap()
    val width = boundingBox.width().toDouble() * scaleX
    val height = boundingBox.height().toDouble() * scaleY
    val x = boundingBox.left.toDouble() * scaleX
    val y = boundingBox.top.toDouble() * scaleY

    when(orientation) {
      Orientation.PORTRAIT -> {
        // device is landscape left
        bounds["x"] = (-y + sourceWidth * scaleX) - width
        bounds["y"] = (-x + sourceHeight * scaleY) - height
      }
      Orientation.LANDSCAPE_LEFT -> {
        // device is portrait
        bounds["x"] = (-x + sourceWidth * scaleX) - width
        bounds["y"] = y
      }
      Orientation.PORTRAIT_UPSIDE_DOWN -> {
        // device is landscape right
        bounds["x"] = y
        bounds["y"] = x
      }
      Orientation.LANDSCAPE_RIGHT -> {
        // device is upside down
        bounds["x"] = x
        bounds["y"] = (-y + sourceHeight * scaleY) - height
      }
    }

    bounds["width"] = width
    bounds["height"] = height
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
      Log.d(
        TAG,
        "Getting '$landmarkName' landmark"
      )
      if (landmark == null) {
        Log.d(
          TAG,
          "Landmark '$landmarkName' is null - going next"
        )
        continue
      }
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
      Log.d(
        TAG,
        "Getting '$contourName' contour"
      )
      if (contour == null) {
        Log.d(
          TAG,
          "Face contour '$contourName' is null - going next"
        )
        continue
      }
      val points = contour.points
      val pointsMap: MutableMap<String, Map<String, Double>> = HashMap()
      for (j in points.indices) {
        val currentPointsMap: MutableMap<String, Double> = HashMap()
        currentPointsMap["x"] = points[j].x.toDouble() * scaleX
        currentPointsMap["y"] = points[j].y.toDouble() * scaleY
        pointsMap[j.toString()] = currentPointsMap
      }

      faceContoursTypesMap[contourName] = pointsMap
    }
    return faceContoursTypesMap
  }

  private fun getOrientation(
    orientation: Orientation
  ): Int {
    return when (orientation) {
      // device is landscape left
      Orientation.PORTRAIT -> 0
      // device is portrait
      Orientation.LANDSCAPE_LEFT -> 270
      // device is landscape right
      Orientation.PORTRAIT_UPSIDE_DOWN -> 180
      // device is upside-down
      Orientation.LANDSCAPE_RIGHT -> 90
    }
  }

  override fun callback(
    frame: Frame,
    params: Map<String, Any>?
  ): Any {
    val result = ArrayList<Map<String, Any>>()
    
    try {
      val orientation = getOrientation(frame.orientation)
      val image = InputImage.fromMediaImage(frame.image, orientation)
      val width =  image.height.toDouble()
      val height =  image.width.toDouble()
      val scaleX = if(autoScale) windowWidth / width else 1.0
      val scaleY = if(autoScale) windowHeight / height else 1.0

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
          frame.orientation,
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
