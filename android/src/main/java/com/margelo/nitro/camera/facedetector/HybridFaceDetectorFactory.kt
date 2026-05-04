package com.margelo.nitro.camera.facedetector

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.camera.HybridCameraOutputSpec

@DoNotStrip
@Keep
class HybridFaceDetectorFactory : HybridFaceDetectorFactorySpec() {
  @DoNotStrip
  @Keep
  override fun createFaceDetector(options: FaceDetectorOptions): HybridFaceDetectorSpec {
    return HybridFaceDetector(options)
  }

  @DoNotStrip
  @Keep
  override fun createFaceDetectorOutput(options: FaceDetectorOutputOptions): HybridCameraOutputSpec {
    return HybridFaceDetectorOutput(options)
  }
}
