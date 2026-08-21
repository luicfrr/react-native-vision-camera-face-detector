package com.margelo.nitro.camera.facedetector


import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark

data class FaceProcessConfig(
  val frameWidth: Double,
  val frameHeight: Double,
  val pointTransformer: (Double, Double) -> Pair<Double, Double>,
  val runLandmarks: Boolean,
  val runContours: Boolean,
  val runClassifications: Boolean,
  val trackingEnabled: Boolean
)

fun createIdentityPointTransformer(): (Double, Double) -> Pair<Double, Double> = { x, y ->
  Pair(x, y)
}

fun createFaceProcessConfig(
  frameWidth: Double,
  frameHeight: Double,
  autoMode: Boolean,
  pointTransformer: (Double, Double) -> Pair<Double, Double>,
  runLandmarks: Boolean,
  runContours: Boolean,
  runClassifications: Boolean,
  trackingEnabled: Boolean
): FaceProcessConfig {
  return FaceProcessConfig(
    frameWidth = frameWidth,
    frameHeight = frameHeight,
    pointTransformer = if (autoMode) pointTransformer else createIdentityPointTransformer(),
    runLandmarks = runLandmarks,
    runContours = runContours,
    runClassifications = runClassifications,
    trackingEnabled = trackingEnabled
  )
}

class HybridFace(
  private val face: Face,
  private val config: FaceProcessConfig
) : HybridFaceSpec() {
  private fun transformPoint(
    x: Double,
    y: Double
  ): Pair<Double, Double> {
    return config.pointTransformer(x, y)
  }

  private fun processBoundingBox(
    boundingBox: Rect
  ): Bounds {
    val points = arrayOf(
      transformPoint(boundingBox.left.toDouble(), boundingBox.top.toDouble()),
      transformPoint(boundingBox.right.toDouble(), boundingBox.top.toDouble()),
      transformPoint(boundingBox.left.toDouble(), boundingBox.bottom.toDouble()),
      transformPoint(boundingBox.right.toDouble(), boundingBox.bottom.toDouble())
    )
    val minX = points.minOf { it.first }
    val maxX = points.maxOf { it.first }
    val minY = points.minOf { it.second }
    val maxY = points.maxOf { it.second }

    return Bounds(
      x = minX,
      y = minY,
      width = maxX - minX,
      height = maxY - minY
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
        pos.y.toDouble()
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
          p.y.toDouble()
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

  override val frameWidth: Double
    get() = config.frameWidth

  override val frameHeight: Double
    get() = config.frameHeight
}
