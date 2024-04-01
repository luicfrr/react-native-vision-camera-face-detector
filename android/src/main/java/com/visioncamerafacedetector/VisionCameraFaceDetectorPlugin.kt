package com.visioncamerafacedetector

import android.content.res.Resources
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
import com.mrousavy.camera.frameprocessor.Frame
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin
import com.mrousavy.camera.frameprocessor.VisionCameraProxy

private const val TAG = "FaceDetector"
class VisionCameraFaceDetectorPlugin(
  proxy: VisionCameraProxy,
  options: Map<String, Any>?
) : FrameProcessorPlugin() {
  private var faceDetector: FaceDetector? = null

  // device display data
  private val density = Resources.getSystem().displayMetrics.density.toInt()
  private val windowWidth = Resources.getSystem().displayMetrics.widthPixels / density
  private val windowHeight = Resources.getSystem().displayMetrics.heightPixels / density

  private fun initFD(params: Map<String, Any>?) {
    var performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_FAST
    var landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_NONE
    var classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_NONE
    var contourModeValue = FaceDetectorOptions.CONTOUR_MODE_NONE
    var minFaceSize = 0.15f
    var enableTracking = false

    if (params?.get("performanceMode").toString() == "accurate") {
      performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
    }

    if (params?.get("landmarkMode").toString() == "all") {
      landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_ALL
    }

    if (params?.get("classificationMode").toString() == "all") {
      classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_ALL
    }

    if (params?.get("contourMode").toString() == "all") {
      contourModeValue = FaceDetectorOptions.CONTOUR_MODE_ALL
    }

    val minFaceSizeParam = params?.get("minFaceSize").toString()
    if (
      minFaceSizeParam != "null" &&
      minFaceSizeParam != minFaceSize.toString()
    ) {
      minFaceSize = minFaceSizeParam.toFloat()
    }

    if (params?.get("trackingEnabled").toString() == "true") {
      enableTracking = true
    }

    val optionsBuilder = FaceDetectorOptions.Builder()
      .setPerformanceMode(performanceModeValue)
      .setLandmarkMode(landmarkModeValue)
      .setContourMode(contourModeValue)
      .setClassificationMode(classificationModeValue)
      .setMinFaceSize(minFaceSize)

    if (enableTracking) {
      optionsBuilder.enableTracking()
    }

    val options = optionsBuilder.build()
    faceDetector = FaceDetection.getClient(options)
  }

  private fun processBoundingBox(
    boundingBox: Rect,
    scaleX: Double,
    scaleY: Double
  ): Map<String, Any> {
    val bounds: MutableMap<String, Any> = HashMap()
    val width = boundingBox.width().toDouble() * scaleX

    bounds["width"] = width
    bounds["height"] = boundingBox.height().toDouble() * scaleY
    bounds["x"] = windowWidth - (width + (
      boundingBox.left.toDouble() * scaleX
    ))
    bounds["y"] = boundingBox.top.toDouble() * scaleY

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

  override fun callback(
    frame: Frame,
    params: Map<String, Any>?
  ): Map<String, Any> {
    val resultMap: MutableMap<String, Any> = HashMap()

    try {
      val frameImage = frame.image
      val orientation = frame.orientation

      if (
        frameImage == null &&
        orientation == null
      ) {
        Log.i(TAG, "Image or orientation is null")
        return resultMap
      }

      if (faceDetector == null) {
        initFD(params)
      }

      val rotation = orientation!!.toDegrees()
      val image = InputImage.fromMediaImage(frameImage!!, rotation)
      val scaleX: Double
      val scaleY: Double
      if (rotation == 270 || rotation == 90) {
        scaleX = windowWidth.toDouble() / image.height
        scaleY = windowHeight.toDouble() / image.width
      } else {
        scaleX = windowWidth.toDouble() / image.width
        scaleY = windowHeight.toDouble() / image.height
      }

      val task = faceDetector!!.process(image)
      val faces = Tasks.await(task)
      val facesList = ArrayList<Map<String, Any?>>()

      faces.forEach{face ->
        val map: MutableMap<String, Any?> = HashMap()

        if (params?.get("landmarkMode").toString() == "all") {
          map["landmarks"] = processLandmarks(
            face,
            scaleX,
            scaleY
          )
        }

        if (params?.get("classificationMode").toString() == "all") {
          map["leftEyeOpenProbability"] = face.leftEyeOpenProbability?.toDouble() ?: -1
          map["rightEyeOpenProbability"] = face.rightEyeOpenProbability?.toDouble() ?: -1
          map["smilingProbability"] = face.smilingProbability?.toDouble() ?: -1
        }

        if (params?.get("contourMode").toString() == "all") {
          map["contours"] = processFaceContours(
            face,
            scaleX,
            scaleY
          )
        }

        if (params?.get("trackingEnabled").toString() == "true") {
          map["trackingId"] = face.trackingId
        }

        map["rollAngle"] = face.headEulerAngleZ.toDouble()
        map["pitchAngle"] = face.headEulerAngleX.toDouble()
        map["yawAngle"] = face.headEulerAngleY.toDouble()
        map["bounds"] = processBoundingBox(
          face.boundingBox,
          scaleX,
          scaleY
        )
        facesList.add(map)
      }

      val frameMap: MutableMap<String, Any> = HashMap()
      val returnOriginal = params?.get("returnOriginal").toString() == "true"
      val convertFrame = params?.get("convertFrame").toString() == "true"

      if (returnOriginal) {
        frameMap["original"] = frame
      }

      if (convertFrame) {
        frameMap["converted"] = BitmapUtils.convertYuvToRgba(frameImage)
      }

      resultMap["faces"] = facesList
      if(returnOriginal || convertFrame) {
        resultMap["frame"] = frameMap
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error processing face detection: ", e)
    } catch (e: FrameInvalidError) {
      Log.e(TAG, "Frame invalid error: ", e)
    }

    return resultMap
  }
}
