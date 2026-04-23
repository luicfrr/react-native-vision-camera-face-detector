package com.nitro.visioncamerafacedetector

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip

@DoNotStrip
@Keep
class HybridFaceDetectorFactory : HybridFaceDetectorFactorySpec() {
  @DoNotStrip
  @Keep
  override fun createFaceDetector(options: FaceDetectorOptions?): HybridFaceDetectorSpec {
    return HybridFaceDetector(options ?: FaceDetectorOptions())
  }
}
