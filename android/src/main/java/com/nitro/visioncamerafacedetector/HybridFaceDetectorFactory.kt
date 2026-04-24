package com.nitro.visioncamerafacedetector

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.camera.facedetector.FaceDetectorOptions
import com.margelo.nitro.camera.facedetector.HybridFaceDetectorFactorySpec
import com.margelo.nitro.camera.facedetector.HybridFaceDetectorSpec

@DoNotStrip
@Keep
class HybridFaceDetectorFactory : HybridFaceDetectorFactorySpec() {
  @DoNotStrip
  @Keep
  override fun createFaceDetector(options: FaceDetectorOptions): HybridFaceDetectorSpec {
    return HybridFaceDetector(options)
  }
}
