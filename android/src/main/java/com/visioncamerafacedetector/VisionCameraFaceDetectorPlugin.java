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
import com.mrousavy.camera.frameprocessor.VisionCameraProxy;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.gson.Gson;

import com.facebook.react.bridge.Arguments;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import androidx.camera.core.ImageProxy;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

public class VisionCameraFaceDetectorPlugin extends FrameProcessorPlugin {

  private Map processBoundingBox(Rect boundingBox) {
    Map<String, Object> bounds = new HashMap<>();

    // Calculate offset (we need to center the overlay on the target)
    Double offsetX = (boundingBox.exactCenterX() - ceil(boundingBox.width())) / 2.0f;
    Double offsetY = (boundingBox.exactCenterY() - ceil(boundingBox.height())) / 2.0f;

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
    int[] faceContoursTypes = new int[] {
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
      List<Map<String, Double>> pointsArray = new ArrayList<>();

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

  @Nullable
  @Override
  public Object callback(@NonNull Frame frame, @Nullable Map<String, Object> params) {
    @SuppressLint("UnsafeOptInUsageError")
    Image mediaImage = frame.getImage();

    Integer performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_FAST;
    if (String.valueOf(params.get("performanceMode")).equals("accurate")) {
      performanceModeValue = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE;
    }

    Integer landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_NONE;
    if (String.valueOf(params.get("landmarkMode")).equals("all")) {
      landmarkModeValue = FaceDetectorOptions.LANDMARK_MODE_ALL;
    }

    Integer classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_NONE;
    if (String.valueOf(params.get("classificationMode")).equals("all")) {
      classificationModeValue = FaceDetectorOptions.CLASSIFICATION_MODE_ALL;
    }

    Integer contourModeValue = FaceDetectorOptions.CONTOUR_MODE_NONE;
    if (String.valueOf(params.get("contourMode")).equals("all")) {
      contourModeValue = FaceDetectorOptions.CONTOUR_MODE_ALL;
    }

    Float minFaceSize = 0.1f;
    String minFaceSizeParam = String.valueOf(params.get("minFaceSize"));
    if (!minFaceSizeParam.equals(String.valueOf(minFaceSize))) {
      minFaceSize = Float.parseFloat(minFaceSizeParam);
    }

    FaceDetectorOptions.Builder optionsBuilder = new FaceDetectorOptions.Builder()
        .setPerformanceMode(performanceModeValue)
        .setLandmarkMode(landmarkModeValue)
        .setContourMode(contourModeValue)
        .setClassificationMode(classificationModeValue)
        .setMinFaceSize(minFaceSize);

    if (String.valueOf(params.get("trackingEnabled")).equals("true")) {
      optionsBuilder.enableTracking();
    }

    FaceDetectorOptions options = optionsBuilder.build();
    FaceDetector faceDetector = FaceDetection.getClient(options);

    if (mediaImage != null) {
      InputImage image = InputImage.fromMediaImage(mediaImage, frame.getOrientation().toDegrees());
      Task<List<Face>> task = faceDetector.process(image);
      List<Map<String, Object>> faceList = new ArrayList<>();
      Map<String, Object> resultMap = new HashMap<>();
      Gson gson = new Gson();

      try {
        List<Face> faces = Tasks.await(task);
        for (Face face : faces) {
          Map<String, Object> map = new HashMap<>();

          // if(String.valueOf(params.get("landmarkMode")).equals("all")){
          //   map.put("landMarks", processLandMarks(face));
          // }

          if(String.valueOf(params.get("classificationMode")).equals("all")) {
            map.put("leftEyeOpenProbability", (double) face.getLeftEyeOpenProbability());
            map.put("rightEyeOpenProbability", (double) face.getRightEyeOpenProbability());
            map.put("smilingProbability", (double) face.getSmilingProbability());
          }

          if(String.valueOf(params.get("contourMode")).equals("all")){
            map.put("contours", processFaceContours(face));
          }

          if (String.valueOf(params.get("trackingEnabled")).equals("true")) {
            map.put("trackingId", face.getTrackingId());
          }

          map.put("rollAngle", (double) face.getHeadEulerAngleZ());
          map.put("pitchAngle", (double) face.getHeadEulerAngleX());
          map.put("yawAngle", (double) face.getHeadEulerAngleY());
          map.put("bounds", processBoundingBox(face.getBoundingBox()));

          faceList.add(map);
        }

        if (faceList.size() > 0) {
          resultMap.put("faces", gson.toJson(faceList));
          // resultMap.put("frameData", BitmapUtils.convertYuvToRgba(mediaImage));
        }
        return resultMap;
      } catch (Exception e) {
        Log.e("FaceDetector", "Error processing face detection", e);
      }
    }

    return null;
  }

  public VisionCameraFaceDetectorPlugin(@NonNull VisionCameraProxy proxy, @Nullable Map<String, Object> options) {
    super();
  }
}
