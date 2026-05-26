package com.margelo.nitro.camera.facedetector

import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.margelo.nitro.NitroModules
import com.margelo.nitro.camera.CameraOrientation
import com.margelo.nitro.camera.HybridCameraOutputSpec
import com.margelo.nitro.camera.MediaType
import com.margelo.nitro.camera.MirrorMode
import com.margelo.nitro.camera.extensions.surfaceRotation
import com.margelo.nitro.camera.facedetector.extensions.toMLFaceDetectorOptions
import com.margelo.nitro.camera.public.NativeCameraOutput
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class HybridFaceDetectorOutput(
  private val options: FaceDetectorOutputOptions
) : HybridCameraOutputSpec(),
  ImageAnalysis.Analyzer,
  NativeCameraOutput {
  override val mediaType: MediaType = MediaType.VIDEO
  override val mirrorMode: MirrorMode = MirrorMode.AUTO
  override var outputOrientation: CameraOrientation = CameraOrientation.UP
    set(value) {
      field = value
      imageAnalysis?.targetRotation = value.surfaceRotation
    }
  override val currentResolution: com.margelo.nitro.camera.Size?
    get() = imageAnalysis?.resolutionInfo?.resolution?.toSize()
  private val context = NitroModules.applicationContext ?: throw Error("Face Detector - No Context available!")
  private val orientationManager = FaceDetectorOrientation.get(context.applicationContext)
  private val runLandmarks = options.runLandmarks ?: false
  private val runContours = options.runContours ?: false
  private val runClassifications = options.runClassifications ?: false
  private val trackingEnabled = options.trackingEnabled ?: false
  private val autoMode = options.autoMode ?: false
  private val cameraFacing: CameraPosition = options.cameraFacing ?: CameraPosition.FRONT
  private val windowWidth = options.windowWidth ?: 1.0
  private val windowHeight = options.windowHeight ?: 1.0
  private val faceDetector = FaceDetection.getClient(
    options.toMLFaceDetectorOptions()
  )
  private var isBusy = AtomicBoolean(false)
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
        .setOutputImageRotationEnabled(false)
        .setResolutionSelector(resolutionSelector)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    return NativeCameraOutput.PreparedUseCase(imageAnalysis, {
      this.imageAnalysis = imageAnalysis
      imageAnalysis.setAnalyzer(executor, this)
    })
  }

  override fun dispose() {
    orientationManager.stopDeviceOrientationListener()
    faceDetector.close()
    executor.close()
  }

  @OptIn(ExperimentalGetImage::class)
  override fun analyze(imageProxy: ImageProxy) {
    try {
      if (!isBusy.compareAndSet(false, true)) {
        // pipeline is busy. close image & return
        imageProxy.close()
        return
      }

      val mediaImage = imageProxy.image
      if (mediaImage == null) {
        // media image is null - error & return.
        imageProxy.close()
        isBusy.set(false)
        options.onError(Error("`ImageProxy` does not have an `Image`!"))
        return
      }
      val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
      val width = inputImage.height.toDouble()
      val height = inputImage.width.toDouble()
      val scaleX = if(autoMode) windowWidth / width else 1.0
      val scaleY = if(autoMode) windowHeight / height else 1.0
      val config = FaceProcessConfig(
        width,
        height,
        scaleX,
        scaleY,
        runLandmarks,
        runContours,
        runClassifications,
        trackingEnabled,
        autoMode,
        cameraFacing,
        orientation = orientationManager.orientation
      )
      faceDetector
        .process(inputImage)
        .addOnSuccessListener { faces ->
          val hybridFaces =
            faces
              .map { HybridFace(it, config) }
              .toTypedArray<HybridFaceSpec>()
          options.onFacesDetected(hybridFaces)
        }.addOnFailureListener { error ->
          options.onError(error)
        }.addOnCompleteListener {
          imageProxy.close()
          isBusy.set(false)
        }
    } catch (error: Throwable) {
      imageProxy.close()
      isBusy.set(false)
      options.onError(error)
    }
  }
}
