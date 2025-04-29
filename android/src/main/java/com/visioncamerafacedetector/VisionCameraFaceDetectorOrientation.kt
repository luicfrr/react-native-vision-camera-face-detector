package com.visioncamerafacedetector

import android.content.Context
import android.os.Build
import android.view.OrientationEventListener
import android.view.Surface
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.CUPCAKE)
class VisionCameraFaceDetectorOrientation(private val context: Context) {
    var orientation = Surface.ROTATION_0
    private var orientationListener: OrientationEventListener? = null

    init {
        if (orientationListener == null) {
            orientationListener = object : OrientationEventListener(context) {
                override fun onOrientationChanged(rotationDegrees: Int) {
                    orientation = degreesToSurfaceRotation(rotationDegrees)
                }
            }
        }

        orientation = Surface.ROTATION_0
        startDeviceOrientationListener()
    }

    protected fun finalize() {
        stopDeviceOrientationListener()
    }

    private fun startDeviceOrientationListener() {
        stopDeviceOrientationListener()

        if (
            orientationListener != null &&
            orientationListener!!.canDetectOrientation()
        ) {
            println("Enabling device orientation listener")
            orientationListener!!.enable()
        }
    }

    private fun stopDeviceOrientationListener() {
        if (orientationListener != null) {
            println("Disabled device orientation listener")
            orientationListener!!.disable()
        }
    }

    private fun degreesToSurfaceRotation(degrees: Int): Int =
        when (degrees) {
            in 45..135 -> Surface.ROTATION_270
            in 135..225 -> Surface.ROTATION_180
            in 225..315 -> Surface.ROTATION_90
            else -> Surface.ROTATION_0
        }
}
