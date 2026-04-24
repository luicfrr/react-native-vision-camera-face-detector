package com.nitro.visioncamerafacedetector

import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.margelo.nitro.camera.facedetector.FaceDetectorOptions
import com.margelo.nitro.camera.facedetector.HybridImageFaceDetectorFactorySpec
import com.margelo.nitro.camera.facedetector.HybridImageFaceDetectorSpec

@DoNotStrip
@Keep
class HybridImageFaceDetectorFactory : HybridImageFaceDetectorFactorySpec() {
  @DoNotStrip
  @Keep
  override fun createImageFaceDetector(options: FaceDetectorOptions): HybridImageFaceDetectorSpec {
    return HybridImageFaceDetector(options)
  }
}
