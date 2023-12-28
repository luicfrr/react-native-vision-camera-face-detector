package com.visioncamerafacedetector;

import static java.lang.Math.ceil;
import android.annotation.SuppressLint;
import android.media.Image;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mrousavy.camera.frameprocessor.Frame;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.gson.Gson;

import androidx.camera.core.ImageProxy;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class VisionCameraFaceDetectorPlugin extends FrameProcessorPlugin {

  private Map processBoundingBox(Rect boundingBox) {
    Map<String, Object> bounds = new HashMap<>();

    // Calculate offset (we need to center the overlay on the target)
    Double offsetX =  (boundingBox.exactCenterX() - ceil(boundingBox.width())) / 2.0f;
    Double offsetY =  (boundingBox.exactCenterY() - ceil(boundingBox.height())) / 2.0f;

    Double x = boundingBox.right + offsetX;
    Double y = boundingBox.top + offsetY;

    bounds.put("x", (double) boundingBox.centerX() + (boundingBox.centerX() - x));
    bounds.put("y", (double) boundingBox.centerY() + (y - boundingBox.centerY()));
    bounds.put("top", (double) boundingBox.top);
    bounds.put("left", (double) boundingBox.left);
    bounds.put("width", (double) boundingBox.width());
    bounds.put("height", (double) boundingBox.height());


    bounds.put("boundingCenterX", boundingBox.centerX());
    bounds.put("boundingCenterY", boundingBox.centerY());
    bounds.put("boundingExactCenterX", boundingBox.exactCenterX());
    bounds.put("boundingExactCenterY", boundingBox.exactCenterY());

    return bounds;
  }
  
 private Map processFaceContours(Face face) {
   int[] faceContoursTypes =
      new int[] {
        FaceContour.FACE,
        FaceContour.LEFT_EYEBROW_TOP,
        FaceContour.LEFT_EYEBROW_BOTTOM,
        FaceContour.RIGHT_EYEBROW_TOP,
        FaceContour.RIGHT_EYEBROW_BOTTOM,
        FaceContour.LEFT_EYE,
        FaceContour.RIGHT_EYE,
        FaceContour.UPPER_LIP_TOP,
        FaceContour.UPPER_LIP_BOTTOM,
        FaceContour.LOWER_LIP_TOP,
        FaceContour.LOWER_LIP_BOTTOM,
        FaceContour.NOSE_BRIDGE,
        FaceContour.NOSE_BOTTOM,
        FaceContour.LEFT_CHEEK,
        FaceContour.RIGHT_CHEEK
      };

    String[] faceContoursTypesStrings = {
        "FACE",
        "LEFT_EYEBROW_TOP",
        "LEFT_EYEBROW_BOTTOM",
        "RIGHT_EYEBROW_TOP",
        "RIGHT_EYEBROW_BOTTOM",
        "LEFT_EYE",
        "RIGHT_EYE",
        "UPPER_LIP_TOP",
        "UPPER_LIP_BOTTOM",
        "LOWER_LIP_TOP",
        "LOWER_LIP_BOTTOM",
        "NOSE_BRIDGE",
        "NOSE_BOTTOM",
        "LEFT_CHEEK",
        "RIGHT_CHEEK"
      };


    Map<String, Object> faceContoursTypesMap = new HashMap<>();

    for (int i = 0; i < faceContoursTypesStrings.length; i++) {
        FaceContour contour = face.getContour(faceContoursTypes[i]);
        List<PointF> points = contour.getPoints();
        List <Map<String, Double>> pointsArray = new ArrayList<>();

        for (int j = 0; j < points.size(); j++) {
            Map<String, Double> currentPointsMap = new HashMap<>();
            currentPointsMap.put("x", (double) points.get(j).x);
            currentPointsMap.put("y", (double) points.get(j).y);
            pointsArray.add(currentPointsMap);
        }
        faceContoursTypesMap.put(faceContoursTypesStrings[contour.getFaceContourType() - 1], pointsArray);
    }

    return faceContoursTypesMap;
  }

  /** Converts a bitmap to base64 format string */
  public static String bitmapToBase64(Bitmap bitmap, Bitmap.CompressFormat format, int quality)
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(format, quality, outputStream);

    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
  }

  public static int convertRotationDegreeFromString(String orientation){
      switch (orientation){
        case "portrait-upside-down":
            return 90;
        case "landscape-left":
            return 0;
        case "landscape-right":
            return 180;
        default:
            return 270;
    }
  }

  @Override
  public Object callback(@NonNull Frame frame, @Nullable Map<String, Object> params) {
    @SuppressLint("UnsafeOptInUsageError")
    Image mediaImage = frame.getImage();

    FaceDetectorOptions options =
    new FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
      .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
      .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
      .setMinFaceSize(0.15f)
      .build();
    
    FaceDetector faceDetector = FaceDetection.getClient(options);

    if (mediaImage != null) {
      InputImage image = InputImage.fromMediaImage(mediaImage, convertRotationDegreeFromString(frame.getOrientation()));
      Task<List<Face>> task = faceDetector.process(image);
      List<Map<String, Object>> faceList = new ArrayList<>();
      Map<String, Object> resultMap = new HashMap<>();
      Gson gson = new Gson();

      Bitmap bitmap = BitmapUtils.convertImageToBitmap(image);
      String frameInBase64 = bitmapToBase64(bitmap, Bitmap.CompressFormat.PNG, 100);

      resultMap.put("frameData", frameInBase64);

      try {
        List<Face> faces = Tasks.await(task);
        for (Face face : faces) {
          Map<String, Object> map = new HashMap<>();

          map.put("rollAngle",(double) face.getHeadEulerAngleZ());
          map.put("pitchAngle",(double) face.getHeadEulerAngleX());
          map.put("yawAngle",(double) face.getHeadEulerAngleY());
          map.put("leftEyeOpenProbability",(double) face.getLeftEyeOpenProbability());
          map.put("rightEyeOpenProbability",(double) face.getRightEyeOpenProbability());
          map.put("smilingProbability",(double) face.getSmilingProbability());
          map.put("contours", processFaceContours(face));
          map.put("bounds", processBoundingBox(face.getBoundingBox()));

          faceList.add(map);
        }

        resultMap.put("faces", gson.toJson(faceList));
        return resultMap;
      } catch (Exception e) {
        Log.e("FaceDetector", "Error processing face detection", e);
      }
    }

    return null;
  }

  VisionCameraFaceDetectorPlugin(@Nullable Map<String, Object> options) {
    super(options);
  }
}
