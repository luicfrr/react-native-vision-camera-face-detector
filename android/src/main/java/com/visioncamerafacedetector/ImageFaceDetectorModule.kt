package com.visioncamerafacedetector

import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.facebook.react.bridge.*
import com.google.mlkit.vision.common.InputImage
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ImageFaceDetector"
class ImageFaceDetectorModule(
  private val reactContext: ReactApplicationContext
): ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "ImageFaceDetector"

  private fun toWritableArray(
    list: ArrayList<Map<String, Any>>
  ): WritableArray {
    val array = Arguments.createArray()

    for (map in list) {
      val writableMap = Arguments.createMap()

      for ((key, value) in map) {
        @Suppress("UNCHECKED_CAST")
        when (value) {
          is Boolean -> writableMap.putBoolean(key, value)
          is Int -> writableMap.putInt(key, value)
          is Double -> writableMap.putDouble(key, value)
          is Float -> writableMap.putDouble(key, value.toDouble())
          is String -> writableMap.putString(key, value)
          is Map<*, *> -> writableMap.putMap(key, toWritableMap(value as Map<String, Any>))
          is ArrayList<*> -> writableMap.putArray(key, toWritableArray(value as ArrayList<Map<String, Any>>))
          else -> writableMap.putNull(key)
        }
      }

      array.pushMap(writableMap)
    }

    return array
  }

  private fun toWritableMap(
    map: Map<String, Any>
  ): WritableMap {
    val writableMap = Arguments.createMap()

    for ((key, value) in map) {
      @Suppress("UNCHECKED_CAST")
      when (value) {
        is Boolean -> writableMap.putBoolean(key, value)
        is Int -> writableMap.putInt(key, value)
        is Double -> writableMap.putDouble(key, value)
        is Float -> writableMap.putDouble(key, value.toDouble())
        is String -> writableMap.putString(key, value)
        is Map<*, *> -> writableMap.putMap(key, toWritableMap(value as Map<String, Any>))
        is ArrayList<*> -> writableMap.putArray(key, toWritableArray(value as ArrayList<Map<String, Any>>))
        else -> writableMap.putNull(key)
      }
    }

    return writableMap
  }

  private fun toMap(
    readableMap: ReadableMap?
  ): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    if (readableMap == null) return map

    val iterator = readableMap.keySetIterator()
    while (iterator.hasNextKey()) {
      val key = iterator.nextKey()
      when (readableMap.getType(key)) {
        ReadableType.Null -> map[key] = ""
        ReadableType.Boolean -> map[key] = readableMap.getBoolean(key)
        ReadableType.Number -> map[key] = readableMap.getDouble(key)
        ReadableType.String -> map[key] = readableMap.getString(key) ?: ""
        ReadableType.Map -> map[key] = toMap(readableMap.getMap(key))
        ReadableType.Array -> map[key] = toList(readableMap.getArray(key))
      }
    }
    return map
  }

  private fun toList(
    readableArray: ReadableArray?
  ): ArrayList<Any> {
    val list = arrayListOf<Any>()
    if (readableArray == null) return list

    for (i in 0 until readableArray.size()) {
      when (readableArray.getType(i)) {
        ReadableType.Null -> list.add("")
        ReadableType.Boolean -> list.add(readableArray.getBoolean(i))
        ReadableType.Number -> list.add(readableArray.getDouble(i))
        ReadableType.String -> list.add(readableArray.getString(i))
        ReadableType.Map -> list.add(toMap(readableArray.getMap(i)))
        ReadableType.Array -> list.add(toList(readableArray.getArray(i)))
      }
    }
    return list
  }

  @ReactMethod
  fun detectFaces(
    uri: String, 
    options: ReadableMap?,
    promise: Promise
  ) {
    try {
      val common = FaceDetectorCommon()
      val (
        runContours,
        runClassifications,
        runLandmarks,
        trackingEnabled,
        faceDetector
      ) = common.getFaceDetector(
        toMap(options)
      )

      val image = InputImage.fromBitmap(
        loadBitmapFromUri(uri)!!,
        0
      )
      faceDetector.process(image)
        .addOnSuccessListener { faces ->
          val result = common.processFaces(
            faces,
            runLandmarks,
            runClassifications,
            runContours,
            trackingEnabled
          )

          promise.resolve(
            toWritableArray(result)
          )
        }
        .addOnFailureListener { e ->
          Log.e(TAG, "Error processing image face detection: ", e)
          // resolve empty list on error
          promise.resolve(Arguments.createArray())
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error preparing face detection: ", e)
      // resolve empty list on error
      promise.resolve(Arguments.createArray())
    }
  }

  private fun loadBitmapFromUri(uriString: String): Bitmap? {
    return try {
      val uri = Uri.parse(uriString)
      when (uri.scheme?.lowercase()) {
        "content", "android.resource" -> {
          val stream = reactContext.contentResolver.openInputStream(uri)
          stream.useDecode()
        }
        "file" -> {
          val path = uri.path ?: return null
          if (path.startsWith("/android_asset/")) {
            val assetPath = path.removePrefix("/android_asset/")
            reactContext.assets.open(assetPath).useDecode()
          } else {
            BitmapFactory.decodeFile(path)
          }
        }
        "asset" -> {
          val assetPath = uriString.removePrefix("asset:/").removePrefix("asset:///")
          reactContext.assets.open(assetPath).useDecode()
        }
        "http", "https" -> {
          val url = URL(uriString)
          val conn = url.openConnection() as HttpURLConnection
          conn.connect()
          val input = conn.inputStream
          input.useDecode()
        }
        else -> {
          // Fallback try as file path
          BitmapFactory.decodeFile(uriString)
        }
      }
    } catch (e: Exception) {
      null
    }
  }
}

private fun InputStream?.useDecode(): Bitmap? {
  if (this == null) return null
  return try {
    this.use { 
      BitmapFactory.decodeStream(it)
    }
  } catch (e: Exception) {
    null
  }
}
