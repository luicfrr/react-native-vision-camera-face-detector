package com.visioncamerafacedetector

import android.util.Log
import android.view.Surface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
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
    windowWidth = (options?.get("windowWidth") ?: 1.0) as Double
    windowHeight = (options?.get("windowHeight") ?: 1.0) as Double

    if (options?.get("cameraFacing").toString() == "back") {
      cameraFacing = Position.BACK
    }

    val faceDetectorResult = common.getFaceDetector(options)
    runLandmarks = faceDetectorResult.runLandmarks
    runClassifications = faceDetectorResult.runClassifications
    runContours = faceDetectorResult.runContours
    trackingEnabled = faceDetectorResult.trackingEnabled
    faceDetector = faceDetectorResult.faceDetector
  }
  

  override fun callback(
    frame: Frame,
    params: Map<String, Any>?
  ): ArrayList<Map<String, Any>> {
    try {
      val image = InputImage.fromMediaImage(
        frame.image,
        frame.imageProxy.imageInfo.rotationDegrees
      )
      // we need to invert sizes as frame is always -90deg rotated
      val width = image.height.toDouble()
      val height = image.width.toDouble()
      val scaleX = if(autoMode) windowWidth / width else 1.0
      val scaleY = if(autoMode) windowHeight / height else 1.0
      val task = faceDetector!!.process(image)
      val faces = Tasks.await(task)

      return common.processFaces(
        faces,
        runLandmarks,
        runClassifications,
        runContours,
        trackingEnabled,
        width,
        height,
        scaleX,
        scaleY,
        autoMode,
        cameraFacing,
        orientationManager.orientation
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error processing face detection: ", e)
    } catch (e: FrameInvalidError) {
      Log.e(TAG, "Frame invalid error: ", e)
    }

    return ArrayList()
  }
}
