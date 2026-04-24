package com.nitro.visioncamerafacedetector

import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.BaseReactPackage
import com.margelo.nitro.camera.facedetector.VisionCameraFaceDetectorOnLoad

class VisionCameraFaceDetectorPackage : BaseReactPackage() {
  override fun getModule(
    name: String, 
    reactContext: ReactApplicationContext
  ): NativeModule? = null

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider { 
    HashMap() 
  }

  companion object {
    init {
      VisionCameraFaceDetectorOnLoad.initializeNative()
    }
  }
}
