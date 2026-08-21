package com.margelo.nitro.camera.facedetector

import android.graphics.Matrix
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.Face
import com.margelo.nitro.camera.CameraOrientation
import com.margelo.nitro.camera.HybridCameraOutputSpec
import com.margelo.nitro.camera.MediaType
import com.margelo.nitro.camera.MirrorMode
import com.margelo.nitro.camera.extensions.converters.toSize
import com.margelo.nitro.camera.extensions.surfaceRotation
import com.margelo.nitro.camera.facedetector.extensions.toMLFaceDetectorOptions
import com.margelo.nitro.camera.public.NativeCameraOutput
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class HybridFaceDetectorOutput(
  private val options: FaceDetectorOutputOptions
) : HybridCameraOutputSpec(),
  ImageAnalysis.Analyzer,
  NativeCameraOutput {
  override val mediaType: MediaType = MediaType.VIDEO
  override var mirrorMode: MirrorMode = MirrorMode.AUTO
  override var outputOrientation: CameraOrientation = CameraOrientation.UP
    set(value) {
      field = value
      imageAnalysis?.targetRotation = value.surfaceRotation
    }
  override val currentResolution: com.margelo.nitro.camera.Size?
    get() = imageAnalysis?.resolutionInfo?.resolution?.toSize()
  private val runLandmarks = options.runLandmarks ?: false
  private val runContours = options.runContours ?: false
  private val runClassifications = options.runClassifications ?: false
  private val trackingEnabled = options.trackingEnabled ?: false
  private val autoMode = options.autoMode ?: false
  private val faceDetector = FaceDetection.getClient(
    options.toMLFaceDetectorOptions()
  )
  private var isBusy = AtomicBoolean(false)
  private val faceDetectorTask = AtomicReference<Task<List<Face>>?>()
  private val executor = Executors.newSingleThreadExecutor()
  private var imageAnalysis: ImageAnalysis? = null
  private val recommendedResolutionForFaceDetection = Size(1280, 720)

  override fun createUseCase(
    mirrorMode: MirrorMode,
    config: NativeCameraOutput.Config,
  ): NativeCameraOutput.PreparedUseCase {
    val resolutionStrategy =
      if (options.outputResolution == FaceDetectorOutputResolution.FULL) {
        ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
      } else {
        ResolutionStrategy(
          recommendedResolutionForFaceDetection,
          ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
      }
    val resolutionSelector =
      ResolutionSelector
        .Builder()
        .setResolutionStrategy(resolutionStrategy)
        .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
        .build()
    val imageAnalysis =
      ImageAnalysis
        .Builder()
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .setTargetRotation(outputOrientation.surfaceRotation)
        .setOutputImageRotationEnabled(false)
        .setResolutionSelector(resolutionSelector)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    return NativeCameraOutput.PreparedUseCase(imageAnalysis) {
      this.imageAnalysis = imageAnalysis
      this.mirrorMode = mirrorMode
      imageAnalysis.setAnalyzer(executor, this)
    }
  }

  override fun dispose() {
    faceDetectorTask.get()?.let {
      try {
        Tasks.await(it)
      } catch (_: Exception) {}
    }

    faceDetector.close()
    executor.close()
    super.dispose()
  }

  @OptIn(ExperimentalGetImage::class)
  override fun analyze(imageProxy: ImageProxy) {
    if (!isBusy.compareAndSet(false, true)) {
      // pipeline is busy. close image & return
      imageProxy.close()
      return
    }

    try {
      val mediaImage = imageProxy.image ?: throw Error("`ImageProxy` does not have an `Image`!")
      val inputImage = InputImage.fromMediaImage(
        mediaImage, 
        imageProxy.imageInfo.rotationDegrees
      )
      val (mlKitFrameWidth, mlKitFrameHeight) = getMLKitCoordinateDimensions(
        inputImage.width.toDouble(),
        inputImage.height.toDouble(),
        imageProxy.imageInfo.rotationDegrees
      )
      // ML Kit rotates results according to InputImage.rotationDegrees. Compose
      // that rotation with CameraX's sensor-to-buffer matrix, then invert the
      // complete transform to obtain ML Kit -> VisionCamera camera coordinates.
      // Capture it before the proxy is closed by the asynchronous detector task.
      val pointTransformer =
        if (autoMode) createMLKitToCameraPointTransformer(imageProxy)
        else createIdentityPointTransformer()
      val config = createFaceProcessConfig(
        frameWidth = mlKitFrameWidth,
        frameHeight = mlKitFrameHeight,
        autoMode = autoMode,
        runLandmarks = runLandmarks,
        runContours = runContours,
        runClassifications = runClassifications,
        trackingEnabled = trackingEnabled,
        pointTransformer = pointTransformer
      )

      val task = faceDetector.process(inputImage)
      faceDetectorTask.set(task)
      
      task.addOnSuccessListener { faces ->
        val hybridFaces =
          faces
            .map { HybridFace(it, config) }
            .toTypedArray<HybridFaceSpec>()
        options.onFacesDetected(hybridFaces)
      }.addOnFailureListener { error ->
        options.onError(error)
      }.addOnCompleteListener {
        imageProxy.close()
        faceDetectorTask.set(null)
        isBusy.set(false)
      }
    } catch (error: Throwable) {
      imageProxy.close()
      isBusy.set(false)
      options.onError(error)
    }
  }

  private fun createMLKitToCameraPointTransformer(
    imageProxy: ImageProxy
  ): (Double, Double) -> Pair<Double, Double> {
    // Mirrors AndroidX MlKitAnalyzer: sensor -> buffer -> ML Kit coordinates.
    val sensorToMLKit = Matrix(imageProxy.imageInfo.sensorToBufferTransformMatrix).apply {
      postConcat(
        createBufferToMLKitRotationMatrix(
          imageProxy.width,
          imageProxy.height,
          imageProxy.imageInfo.rotationDegrees
        )
      )
    }
    val mlKitToSensor = Matrix()
    check(sensorToMLKit.invert(mlKitToSensor)) {
      "Could not invert the CameraX/ML Kit coordinate transform matrix."
    }

    return { x, y ->
      val point = floatArrayOf(x.toFloat(), y.toFloat())
      mlKitToSensor.mapPoints(point)
      Pair(point[0].toDouble(), point[1].toDouble())
    }
  }
}
