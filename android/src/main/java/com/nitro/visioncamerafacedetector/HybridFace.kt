package com.nitro.visioncamerafacedetector


import android.graphics.Rect
import android.view.Surface
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark
import com.margelo.nitro.camera.facedetector.CameraPosition
import com.margelo.nitro.camera.facedetector.Bounds
import com.margelo.nitro.camera.facedetector.Point
import com.margelo.nitro.camera.facedetector.Contours
import com.margelo.nitro.camera.facedetector.HybridFaceSpec
import com.margelo.nitro.camera.facedetector.Landmarks

data class FaceProcessConfig(
  val width: Double,
  val height: Double,
  val scaleX: Double,
  val scaleY: Double,
  val runLandmarks: Boolean,
  val runContours: Boolean,
  val runClassifications: Boolean,
  val trackingEnabled: Boolean,
  val autoMode: Boolean? = null,
  val cameraFacing: CameraPosition? = null,
  val orientation: Int? = null
)

data class Point(
  val x: Double,
  val y: Double
)

class HybridFace(
  private val face: Face,
  private val config: FaceProcessConfig
) : HybridFaceSpec() {
  private fun transformPoint(
    x: Double,
    y: Double,
    width: Double,
    height: Double
  ): Pair<Double, Double> {
    val scaleX = config.scaleX
    val scaleY = config.scaleY

    if (config.autoMode != true) {
      return Pair(x * scaleX, y * scaleY)
    }

    return when (config.cameraFacing) {
      CameraPosition.FRONT -> when (config.orientation) {
        Surface.ROTATION_0 ->
          Pair(
            ((-x * scaleX) + config.width * scaleX) - width, 
            y * scaleY
          )

        Surface.ROTATION_270 ->
          Pair(
            y * scaleX, 
            x * scaleY
          )

        Surface.ROTATION_180 ->
          Pair(
            x * scaleX, 
            ((-y * scaleY) + config.height * scaleY) - height
          )

        Surface.ROTATION_90 ->
          Pair(
            ((-y * scaleX) + config.width * scaleX) - width,
            ((-x * scaleY) + config.height * scaleY) - height
          )

        else ->
          Pair(
            x * scaleX, 
            y * scaleY
          )
      } else -> when (config.orientation) {
        Surface.ROTATION_0 ->
          Pair(
            x * scaleX, 
            y * scaleY
          )

        Surface.ROTATION_270 ->
          Pair(
            y * scaleX, 
            ((-x * scaleY) + config.height * scaleY) - height
          )

        Surface.ROTATION_180 ->
          Pair(
            ((-x * scaleX) + config.width * scaleX) - width,
            ((-y * scaleY) + config.height * scaleY) - height
          )

        Surface.ROTATION_90 ->
          Pair(
            ((-y * scaleX) + config.width * scaleX) - width,
            x * scaleY
          )

        else ->
          Pair(
            x * scaleX, 
            y * scaleY
          )
      }
    }
  }

  private fun processBoundingBox(
    boundingBox: Rect
  ): Bounds {
    val scaleX = config.scaleX
    val scaleY = config.scaleY
    val width = boundingBox.width().toDouble() * scaleX
    val height = boundingBox.height().toDouble() * scaleY
    val (x, y) = transformPoint(
      boundingBox.left.toDouble(), 
      boundingBox.top.toDouble(), 
      width, 
      height
    )

    return Bounds(
      x = x,
      y = y,
      width = width,
      height = height
    )
  }

  private fun processLandmarks(
    face: Face
  ): Landmarks {
    fun getPoint(
      type: Int
    ): Point? {
      val landmark = face.getLandmark(type) ?: return null
      val pos = landmark.position

      val (x, y) = transformPoint(
        pos.x.toDouble(), 
        pos.y.toDouble(), 
        0.0, 
        0.0
      )

      return Point(x, y)
    }

    return Landmarks(
      LEFT_CHEEK = getPoint(FaceLandmark.LEFT_CHEEK),
      LEFT_EAR = getPoint(FaceLandmark.LEFT_EAR),
      LEFT_EYE = getPoint(FaceLandmark.LEFT_EYE),
      MOUTH_BOTTOM = getPoint(FaceLandmark.MOUTH_BOTTOM),
      MOUTH_LEFT = getPoint(FaceLandmark.MOUTH_LEFT),
      MOUTH_RIGHT = getPoint(FaceLandmark.MOUTH_RIGHT),
      NOSE_BASE = getPoint(FaceLandmark.NOSE_BASE),
      RIGHT_CHEEK = getPoint(FaceLandmark.RIGHT_CHEEK),
      RIGHT_EAR = getPoint(FaceLandmark.RIGHT_EAR),
      RIGHT_EYE = getPoint(FaceLandmark.RIGHT_EYE)
    )
  }

  private fun processFaceContours(
    face: Face
  ): Contours {
    fun getContour(
      type: Int
    ): Array<Point>? {
      val contour = face.getContour(type) ?: return null

      return contour.points.map { p ->
        val (x, y) = transformPoint(
          p.x.toDouble(), 
          p.y.toDouble(), 
          0.0, 
          0.0
        )

        Point(x, y)
      }.toTypedArray()
    }

    return Contours(
      FACE = getContour(FaceContour.FACE),
      LEFT_EYE = getContour(FaceContour.LEFT_EYE),
      RIGHT_EYE = getContour(FaceContour.RIGHT_EYE),
      LEFT_CHEEK = getContour(FaceContour.LEFT_CHEEK),
      RIGHT_CHEEK = getContour(FaceContour.RIGHT_CHEEK),
      NOSE_BRIDGE = getContour(FaceContour.NOSE_BRIDGE),
      NOSE_BOTTOM = getContour(FaceContour.NOSE_BOTTOM),
      UPPER_LIP_TOP = getContour(FaceContour.UPPER_LIP_TOP),
      UPPER_LIP_BOTTOM = getContour(FaceContour.UPPER_LIP_BOTTOM),
      LOWER_LIP_TOP = getContour(FaceContour.LOWER_LIP_TOP),
      LOWER_LIP_BOTTOM = getContour(FaceContour.LOWER_LIP_BOTTOM),
      LEFT_EYEBROW_TOP = getContour(FaceContour.LEFT_EYEBROW_TOP),
      LEFT_EYEBROW_BOTTOM = getContour(FaceContour.LEFT_EYEBROW_BOTTOM),
      RIGHT_EYEBROW_TOP = getContour(FaceContour.RIGHT_EYEBROW_TOP),
      RIGHT_EYEBROW_BOTTOM = getContour(FaceContour.RIGHT_EYEBROW_BOTTOM)
    )
  }

  override val bounds: Bounds
    get() = processBoundingBox(
      face.boundingBox
    )

  override val landmarks: Landmarks?
    get() = if (config.runLandmarks)
      processLandmarks(
        face
      )
    else null

  override val contours: Contours?
    get() = if (config.runContours)
      processFaceContours(
        face
      )
    else null

  override val leftEyeOpenProbability: Double?
    get() = if (config.runClassifications)
      face.leftEyeOpenProbability?.toDouble()
    else null

  override val rightEyeOpenProbability: Double?
    get() = if (config.runClassifications)
      face.rightEyeOpenProbability?.toDouble()
    else null

  override val smilingProbability: Double?
    get() = if(config.runClassifications)
      face.smilingProbability?.toDouble()
    else null

  override val trackingId: Double?
    get() = if(config.trackingEnabled)
      face.trackingId?.toDouble()
    else null

  override val pitchAngle: Double
    get() = face.headEulerAngleX.toDouble()

  override val rollAngle: Double
    get() = face.headEulerAngleZ.toDouble()

  override val yawAngle: Double
    get() = face.headEulerAngleY.toDouble()
}
