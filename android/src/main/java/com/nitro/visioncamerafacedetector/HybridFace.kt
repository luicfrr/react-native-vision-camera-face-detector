package com.nitro.visioncamerafacedetector

import com.google.mlkit.vision.face.Face
import com.nitro.visioncamerafacedetector.extensions.fromMLBarcodeFormat
import com.nitro.visioncamerafacedetector.extensions.fromMLBarcodeValueType
import com.margelo.nitro.core.ArrayBuffer

data class FaceProcessConfig(
  val width: Double,
  val height: Double,
  val scaleX: Double,
  val scaleY: Double,
  val runLandmarks: Boolean,
  val runContours: Boolean,
  val runClassifications: Boolean,
  val trackingEnabled: Boolean,
  val autoMode: Boolean?,
  val cameraFacing: Position?,
  val orientation: Surface?
)

data class Point(
  val x: Double,
  val y: Double
)

data class Landmarks(
  val leftCheek: Point?,
  val leftEar: Point?,
  val leftEye: Point?,
  val mouthBottom: Point?,
  val mouthLeft: Point?,
  val mouthRight: Point?,
  val noseBase: Point?,
  val rightCheek: Point?,
  val rightEar: Point?,
  val rightEye: Point?
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

    if (!config.autoMode) {
      return Pair(x * scaleX, y * scaleY)
    }

    return when (config.cameraFacing) {
      Position.FRONT -> when (config.orientation) {
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
      leftCheek = getPoint(FaceLandmark.LEFT_CHEEK),
      leftEar = getPoint(FaceLandmark.LEFT_EAR),
      leftEye = getPoint(FaceLandmark.LEFT_EYE),
      mouthBottom = getPoint(FaceLandmark.MOUTH_BOTTOM),
      mouthLeft = getPoint(FaceLandmark.MOUTH_LEFT),
      mouthRight = getPoint(FaceLandmark.MOUTH_RIGHT),
      noseBase = getPoint(FaceLandmark.NOSE_BASE),
      rightCheek = getPoint(FaceLandmark.RIGHT_CHEEK),
      rightEar = getPoint(FaceLandmark.RIGHT_EAR),
      rightEye = getPoint(FaceLandmark.RIGHT_EYE)
    )
  }

  private fun processFaceContours(
    face: Face
  ): Contours {
    fun getPoint(
      type: Int
    ): List<Point>? {
      val contour = face.getContour(type) ?: return null

      return contour.points.map { p ->
        val (tx, ty) = transformPoint(
          p.x.toDouble(), 
          p.y.toDouble(), 
          0.0, 
          0.0
        )

        Point(x, y)
      }
    }

    return Contours(
      face = getContour(FaceContour.FACE),
      leftEye = getContour(FaceContour.LEFT_EYE),
      rightEye = getContour(FaceContour.RIGHT_EYE),
      leftCheek = getContour(FaceContour.LEFT_CHEEK),
      rightCheek = getContour(FaceContour.RIGHT_CHEEK),
      noseBridge = getContour(FaceContour.NOSE_BRIDGE),
      noseBottom = getContour(FaceContour.NOSE_BOTTOM),
      upperLipTop = getContour(FaceContour.UPPER_LIP_TOP),
      upperLipBottom = getContour(FaceContour.UPPER_LIP_BOTTOM),
      lowerLipTop = getContour(FaceContour.LOWER_LIP_TOP),
      lowerLipBottom = getContour(FaceContour.LOWER_LIP_BOTTOM),
      leftEyebrowTop = getContour(FaceContour.LEFT_EYEBROW_TOP),
      leftEyebrowBottom = getContour(FaceContour.LEFT_EYEBROW_BOTTOM),
      rightEyebrowTop = getContour(FaceContour.RIGHT_EYEBROW_TOP),
      rightEyebrowBottom = getContour(FaceContour.RIGHT_EYEBROW_BOTTOM)
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
