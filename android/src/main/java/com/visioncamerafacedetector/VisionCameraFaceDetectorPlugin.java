package com.visioncamerafacedetector;

import static java.lang.Math.ceil;
import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.mrousavy.camera.frameprocessor.Frame;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;
import com.mrousavy.camera.frameprocessor.VisionCameraProxy;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

public class VisionCameraFaceDetectorPlugin extends FrameProcessorPlugin {
  private static final String TAG = "FaceDetector";
  private FaceDetector faceDetector = null;

  private void initFD(@Nullable Map<String, Object> params) {
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

    Float minFaceSize = 0.15f;
    String minFaceSizeParam = String.valueOf(params.get("minFaceSize"));
    if (
      !minFaceSizeParam.equals("null") &&
      !minFaceSizeParam.equals(String.valueOf(minFaceSize))
    ) {
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
    faceDetector = FaceDetection.getClient(options);
  }

  private Map processBoundingBox(Rect boundingBox) {
    Map<String, Object> bounds = new HashMap<>();

    bounds.put("width", (double) boundingBox.width());
    bounds.put("height", (double) boundingBox.height());
    bounds.put("top", (double) boundingBox.top);
    bounds.put("left", (double) boundingBox.left);
    bounds.put("right", (double) boundingBox.right);
    bounds.put("bottom", (double) boundingBox.bottom);
    bounds.put("centerX", (double) boundingBox.centerX());
    bounds.put("centerY", (double) boundingBox.centerY());

    return bounds;
  }

  private Map processLandmarks(Face face) {
    int[] faceLandmarksTypes = new int[] {
      FaceLandmark.LEFT_CHEEK,
      FaceLandmark.LEFT_EAR,
      FaceLandmark.LEFT_EYE,
      FaceLandmark.MOUTH_BOTTOM,
      FaceLandmark.MOUTH_LEFT,
      FaceLandmark.MOUTH_RIGHT,
      FaceLandmark.NOSE_BASE,
      FaceLandmark.RIGHT_CHEEK,
      FaceLandmark.RIGHT_EAR,
      FaceLandmark.RIGHT_EYE
    };

    String[] faceLandmarksTypesStrings = {
      "LEFT_CHEEK",
      "LEFT_EAR",
      "LEFT_EYE",
      "MOUTH_BOTTOM",
      "MOUTH_LEFT",
      "MOUTH_RIGHT",
      "NOSE_BASE",
      "RIGHT_CHEEK",
      "RIGHT_EAR",
      "RIGHT_EYE"
    };

    Map<String, Object> faceLandmarksTypesMap = new HashMap<>();
    for (int i = 0; i < faceLandmarksTypesStrings.length; i++) {
      FaceLandmark landmark = face.getLandmark(faceLandmarksTypes[i]);

      if(landmark != null) {
        PointF point = landmark.getPosition();
        faceLandmarksTypesMap.put(faceLandmarksTypesStrings[i], point);
      }
    }

    return faceLandmarksTypesMap;
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
    Image mediaImage = frame.getImage();

    if (mediaImage != null) {
      if(faceDetector == null) {
        initFD(params);
      }

      int frameOrientation = (frame.getOrientation().toDegrees() - 90 + 360) % 360;
      InputImage image = InputImage.fromMediaImage(mediaImage, frameOrientation);
      Task<List<Face>> task = faceDetector.process(image);
      List<Map<String, Object>> faceList = new ArrayList<>();
      Map<String, Object> resultMap = new HashMap<>();
      Gson gson = new Gson();

      try {
        List<Face> faces = Tasks.await(task);
        for (Face face : faces) {
          Map<String, Object> map = new HashMap<>();

          if(String.valueOf(params.get("landmarkMode")).equals("all")){
            map.put("landmarks", processLandmarks(face));
          }

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

        resultMap.put("faces", faceList);
        if (String.valueOf(params.get("convertFrame")).equals("true")) {
          resultMap.put("frameData", BitmapUtils.convertYuvToRgba(mediaImage));
        }
      } catch (Exception e) {
        Log.e(TAG, "Error processing face detection: ", e);
      }
      return gson.toJson(resultMap)
    }

    return null;
  }

  public VisionCameraFaceDetectorPlugin(@NonNull VisionCameraProxy proxy, @Nullable Map<String, Object> options) {
    super();
  }
}
