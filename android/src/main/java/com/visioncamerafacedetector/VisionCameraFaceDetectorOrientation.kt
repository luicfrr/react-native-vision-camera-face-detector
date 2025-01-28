package com.visioncamerafacedetector

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface

class VisionCameraFaceDetectorOrientation(private val context: Context) {
    var orientation = Surface.ROTATION_0
    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(rotationDegrees: Int) {
            orientation = degreesToSurfaceRotation(rotationDegrees)
        }
    }

    init {
        orientation = Surface.ROTATION_0
        startDeviceOrientationListener()
    }

    protected fun finalize() {
        stopDeviceOrientationListener()
    }

    private fun startDeviceOrientationListener() {
        stopDeviceOrientationListener()

        if(orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
    }

    private fun stopDeviceOrientationListener() {
        orientationListener.disable()
    }

    private fun degreesToSurfaceRotation(degrees: Int): Int =
        when (degrees) {
            in 45..135 -> Surface.ROTATION_270
            in 135..225 -> Surface.ROTATION_180
            in 225..315 -> Surface.ROTATION_90
            else -> Surface.ROTATION_0
        }
}
