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

import com.mrousavy.camera.types.Orientation;
import com.mrousavy.camera.core.FrameInvalidError;
import com.mrousavy.camera.frameprocessor.Frame;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;
import com.mrousavy.camera.frameprocessor.VisionCameraProxy;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

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
      String landmarkName = faceLandmarksTypesStrings[i];

      Log.d(TAG, "Getting '" + landmarkName + "' landmark");  
      if(landmark == null) {
        Log.d(TAG, "Landmark '" + landmarkName + "' is null - going next");
        continue;
      }
      
      PointF point = landmark.getPosition();
      Map<String, Double> currentPointsMap = new HashMap<>();
      currentPointsMap.put("x", (double) point.x);
      currentPointsMap.put("y", (double) point.y);

      faceLandmarksTypesMap.put(landmarkName, currentPointsMap);
    }

    return faceLandmarksTypesMap;
  }
  
  private Map processFaceContours(Face face) {
    int[] faceContoursTypes = new int[] {
      FaceContour.FACE,
      FaceContour.LEFT_CHEEK,
      FaceContour.LEFT_EYE,
      FaceContour.LEFT_EYEBROW_BOTTOM,
      FaceContour.LEFT_EYEBROW_TOP,
      FaceContour.LOWER_LIP_BOTTOM,
      FaceContour.LOWER_LIP_TOP,
      FaceContour.NOSE_BOTTOM,
      FaceContour.NOSE_BRIDGE,
      FaceContour.RIGHT_CHEEK,
      FaceContour.RIGHT_EYE,
      FaceContour.RIGHT_EYEBROW_BOTTOM,
      FaceContour.RIGHT_EYEBROW_TOP,
      FaceContour.UPPER_LIP_BOTTOM,
      FaceContour.UPPER_LIP_TOP
    };

    String[] faceContoursTypesStrings = {
      "FACE",
      "LEFT_CHEEK",
      "LEFT_EYE",
      "LEFT_EYEBROW_BOTTOM",
      "LEFT_EYEBROW_TOP",
      "LOWER_LIP_BOTTOM",
      "LOWER_LIP_TOP",
      "NOSE_BOTTOM",
      "NOSE_BRIDGE",
      "RIGHT_CHEEK",
      "RIGHT_EYE",
      "RIGHT_EYEBROW_BOTTOM",
      "RIGHT_EYEBROW_TOP",
      "UPPER_LIP_BOTTOM",
      "UPPER_LIP_TOP"
    };

    Map<String, Object> faceContoursTypesMap = new HashMap<>();
    for (int i = 0; i < faceContoursTypesStrings.length; i++) {
      FaceContour contour = face.getContour(faceContoursTypes[i]);
      String contourName = faceContoursTypesStrings[i];

      Log.d(TAG, "Getting '" + contourName + "' contour");
      if(contour == null) {
        Log.d(TAG, "Face contour '" + contourName + "' is null - going next");
        continue;
      }

      List<PointF> points = contour.getPoints();
      Map<String, Map<String, Double>> pointsMap = new HashMap<>();
      for (int j = 0; j < points.size(); j++) {
        Map<String, Double> currentPointsMap = new HashMap<>();
        currentPointsMap.put("x", (double) points.get(j).x);
        currentPointsMap.put("y", (double) points.get(j).y);
        pointsMap.put(String.valueOf(j), currentPointsMap);
      }

      faceContoursTypesMap.put(contourName, pointsMap);
    }

    return faceContoursTypesMap;
  }

  @Nullable
  @Override
  public Object callback(@NonNull Frame frame, @Nullable Map<String, Object> params) {
    Image mediaImage = null;
    Orientation orientation = null;
    Map<String, Object> resultMap = new HashMap<>();

    try {
       mediaImage = frame.getImage();
    } catch (Error e) {
      Log.e(TAG, "Error getting frame image: ", e);
    }

    try {
      orientation = frame.getOrientation();
    } catch (FrameInvalidError e) {
      Log.e(TAG, "Error getting frame orientation: ", e);
    }

    if (
      mediaImage != null &&
      orientation != null
    ) {
      try {
        if(faceDetector == null) {
          initFD(params);
        }

        int fixedOrientation = (orientation.toDegrees() - 90 + 360) % 360;
        InputImage image = InputImage.fromMediaImage(mediaImage, fixedOrientation);
        Task<List<Face>> task = faceDetector.process(image);
        List<Face> faces = Tasks.await(task);
        Map<String, Object> facesMap = new HashMap<>();
        for (int i = 0; i < faces.size(); i++) {
          Face face = faces.get(i);
          Map<String, Object> map = new HashMap<>();

          if(String.valueOf(params.get("landmarkMode")).equals("all")){
            map.put("landmarks", processLandmarks(face));
          }

          if(String.valueOf(params.get("classificationMode")).equals("all")) {
            double leftEyeOpenProbability = face.getLeftEyeOpenProbability() != null ? 
              (double) face.getLeftEyeOpenProbability() : -1;
            map.put("leftEyeOpenProbability", leftEyeOpenProbability);

            double rightEyeOpenProbability = face.getRightEyeOpenProbability() != null ? 
              (double) face.getRightEyeOpenProbability() : -1;
            map.put("rightEyeOpenProbability", rightEyeOpenProbability);
        
            double smilingProbability = face.getSmilingProbability() != null ? 
              (double) face.getSmilingProbability() : -1;
            map.put("smilingProbability", smilingProbability);
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

          facesMap.put(String.valueOf(i), map);
        }
        
        Map<String, Object> frameMap = new HashMap<>();
        frameMap.put("original", frame);
        frameMap.put("width", mediaImage.getWidth());
        frameMap.put("height", mediaImage.getHeight());
        frameMap.put("orientation", orientation.getUnionValue());
        if (String.valueOf(params.get("convertFrame")).equals("true")) {
          frameMap.put("frameData", BitmapUtils.convertYuvToRgba(mediaImage));
        }

        resultMap.put("faces", facesMap);
        resultMap.put("frame", frameMap);
      } catch (Exception e) {
        Log.e(TAG, "Error processing face detection: ", e);
      }
      
      return resultMap;
    }

    return resultMap;
  }

  public VisionCameraFaceDetectorPlugin(@NonNull VisionCameraProxy proxy, @Nullable Map<String, Object> options) {
    super();
  }
}
