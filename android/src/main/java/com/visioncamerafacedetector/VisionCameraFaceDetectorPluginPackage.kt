package com.visioncamerafacedetector

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.uimanager.ViewManager
import com.mrousavy.camera.frameprocessors.FrameProcessorPluginRegistry

class VisionCameraFaceDetectorPluginPackage: ReactPackage {
  companion object {
    private var orientationManager: VisionCameraFaceDetectorOrientation? = null

    init {
      FrameProcessorPluginRegistry.addFrameProcessorPlugin("detectFaces") { proxy, options ->
        if(orientationManager == null) {
          orientationManager = VisionCameraFaceDetectorOrientation(proxy.context)
        }
        VisionCameraFaceDetectorPlugin(options, orientationManager!!)
      }
    }

    fun stopDeviceOrientationListener() {
      orientationManager?.stopDeviceOrientationListener()
      orientationManager = null
    }
  }

  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    return listOf(VisionCameraFaceDetectorOrientationManager(reactContext))
  }


  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    return emptyList()
  }
}

class VisionCameraFaceDetectorOrientationManager(context: ReactApplicationContext) :
  ReactContextBaseJavaModule(context) {
  override fun getName(): String {
    return "VisionCameraFaceDetectorOrientationManager"
  }

  @ReactMethod
  fun stopDeviceOrientationListener() {
    VisionCameraFaceDetectorPluginPackage.stopDeviceOrientationListener()
  }
}
