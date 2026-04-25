package com.margelo.nitro.camera.facedetector

import android.content.Context
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface

private const val TAG = "FaceDetectorOrientation"
class FaceDetectorOrientationListener(
  context: Context
) {
  var orientation = Surface.ROTATION_0
  private var orientationListener: OrientationEventListener? = null

  init {
    Log.d(TAG, "Assigning new device orientation listener")

    orientationListener = object : OrientationEventListener(context.applicationContext) {
       override fun onOrientationChanged(rotationDegrees: Int) {
        orientation = degreesToSurfaceRotation(rotationDegrees)
      }
    }

    startDeviceOrientationListener()
  }

  private fun startDeviceOrientationListener() {
    orientationListener?.let {
      if (it.canDetectOrientation()) {
        Log.d(TAG, "Enabling device orientation listener")
        it.enable()
      }
    }
  }

  fun stopDeviceOrientationListener() {
    orientationListener?.disable()
    orientationListener = null
    Log.d(TAG, "Disabled device orientation listener")
  }

  private fun degreesToSurfaceRotation(degrees: Int): Int =
    when (degrees) {
      in 45..135 -> Surface.ROTATION_270
      in 135..225 -> Surface.ROTATION_180
      in 225..315 -> Surface.ROTATION_90
      else -> Surface.ROTATION_0
    }
}

object FaceDetectorOrientation {
  private var instance: FaceDetectorOrientationListener? = null

  fun get(context: Context): FaceDetectorOrientationListener {
    if (instance == null) {
      instance = FaceDetectorOrientationListener(context.applicationContext)
    }
    return instance!!
  }

  fun stop() {
    instance?.stopDeviceOrientationListener()
    instance = null
  }
}
