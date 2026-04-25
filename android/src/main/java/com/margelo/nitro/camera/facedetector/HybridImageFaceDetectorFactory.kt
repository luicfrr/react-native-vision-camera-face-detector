package com.margelo.nitro.camera.facedetector

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip

@DoNotStrip
@Keep
class HybridImageFaceDetectorFactory : HybridImageFaceDetectorFactorySpec() {
  @DoNotStrip
  @Keep
  override fun createImageFaceDetector(options: FaceDetectorOptions): HybridImageFaceDetectorSpec {
    return HybridImageFaceDetector(options)
  }
}
