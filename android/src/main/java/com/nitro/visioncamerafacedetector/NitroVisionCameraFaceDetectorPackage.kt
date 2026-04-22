package com.nitro.VisionCameraFaceDetector

import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.BaseReactPackage

class VisionCameraFaceDetectorPackage : BaseReactPackage() {
  override fun getModule(
    name: String, 
    reactContext: ReactApplicationContext
  ): NativeModule? = null

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider { 
    HashMap() 
  }

  companion object {
    private var orientationManager: VisionCameraFaceDetectorOrientation? = null

    init {
      VisionCameraFaceDetectorOnLoad.initializeNative()
    }
  }
}
