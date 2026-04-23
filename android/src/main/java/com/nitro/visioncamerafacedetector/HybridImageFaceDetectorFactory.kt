package com.nitro.visioncamerafacedetector

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip

@DoNotStrip
@Keep
class HybridImageFaceDetectorFactory : HybridImageFaceDetectorFactorySpec() {
  @DoNotStrip
  @Keep
  override fun createImageFaceDetector(options: ImageFaceDetectorOptions?): HybridImageFaceDetectorSpec {
    return HybridImageFaceDetector(options ?: ImageFaceDetectorOptions())
  }
}
