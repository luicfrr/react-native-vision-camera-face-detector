import MLKitFaceDetection
import AVFoundation

struct FaceDetectorResult {
    let runContours: Bool
    let runClassifications: Bool
    let runLandmarks: Bool
    let trackingEnabled: Bool
    let faceDetector: FaceDetector
}

final class FaceDetectorCommon {
  func getConfig(
    withArguments arguments: [AnyHashable: Any]?
  ) -> [String:Any] {
    guard let arguments = arguments, !arguments.isEmpty else {
      return [:]
    }

    let config = Dictionary(uniqueKeysWithValues: arguments.map {
      (key, value) in (key as? String ?? "", value)
    })

    return config
  }

  private func processBoundingBox(
    from face: Face,
    sourceWidth: CGFloat = 0.0,
    sourceHeight: CGFloat = 0.0,
    scaleX: CGFloat = 1.0,
    scaleY: CGFloat = 1.0,
    autoMode: Bool = false
  ) -> [String:Any] {
    let boundingBox = face.frame
    let width = boundingBox.width * scaleX
    let height = boundingBox.height * scaleY
    // inverted because we also inverted sourceWidth/height
    let x = boundingBox.origin.y * scaleX
    let y = boundingBox.origin.x * scaleY
    
    if(autoMode) {
      return [
        "width": width,
        "height": height,
        "x": (-x + sourceWidth * scaleX) - width,
        "y": y
      ]
    }
    
    return [
      "width": width,
      "height": height,
      "x": y,
      "y": x
    ]
  }

  private func processLandmarks(
    from face: Face,
    sourceWidth: CGFloat = 0.0,
    sourceHeight: CGFloat = 0.0,
    scaleX: CGFloat = 1.0,
    scaleY: CGFloat = 1.0,
    autoMode: Bool = false
  ) -> [String:[String: CGFloat?]] {
    let faceLandmarkTypes = [
      FaceLandmarkType.leftCheek,
      FaceLandmarkType.leftEar,
      FaceLandmarkType.leftEye,
      FaceLandmarkType.mouthBottom,
      FaceLandmarkType.mouthLeft,
      FaceLandmarkType.mouthRight,
      FaceLandmarkType.noseBase,
      FaceLandmarkType.rightCheek,
      FaceLandmarkType.rightEar,
      FaceLandmarkType.rightEye
    ]

    let faceLandmarksTypesStrings = [
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
    ];

    var faceLandMarksTypesMap: [String: [String: CGFloat?]] = [:]
    for i in 0..<faceLandmarkTypes.count {
      let landmark = face.landmark(ofType: faceLandmarkTypes[i]);
      // inverted because we also inverted sourceWidth/height
      let x = landmark?.position.y ?? 0.0 * scaleX
      let y = landmark?.position.x ?? 0.0 * scaleY

      var position: [String: CGFloat] 
      if autoMode {
        position = [
          "x": (-x + sourceWidth * scaleX),
          "y": y
        ]
      } else {
        position = [
          "x": y,
          "y": x
        ]
      }
      
      faceLandMarksTypesMap[faceLandmarksTypesStrings[i]] = position
    }

    return faceLandMarksTypesMap
  }

  private func processFaceContours(
    from face: Face,
    sourceWidth: CGFloat = 0.0,
    sourceHeight: CGFloat = 0.0,
    scaleX: CGFloat = 1.0,
    scaleY: CGFloat = 1.0,
    autoMode: Bool = false,
    cameraFacing: AVCaptureDevice.Position = .front,
    orientation: UIDeviceOrientation = .portrait
  ) -> [String:[[String:CGFloat]]] {
    let faceContoursTypes = [
      FaceContourType.face,
      FaceContourType.leftCheek,
      FaceContourType.leftEye,
      FaceContourType.leftEyebrowBottom,
      FaceContourType.leftEyebrowTop,
      FaceContourType.lowerLipBottom,
      FaceContourType.lowerLipTop,
      FaceContourType.noseBottom,
      FaceContourType.noseBridge,
      FaceContourType.rightCheek,
      FaceContourType.rightEye,
      FaceContourType.rightEyebrowBottom,
      FaceContourType.rightEyebrowTop,
      FaceContourType.upperLipBottom,
      FaceContourType.upperLipTop
    ]

    let faceContoursTypesStrings = [
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
    ];

    var faceContoursTypesMap: [String:[[String:CGFloat]]] = [:]
    for i in 0..<faceContoursTypes.count {
      let contour = face.contour(ofType: faceContoursTypes[i])

      var pointsArray: [[String:CGFloat]] = []
      if let points = contour?.points {
        for point in points {
          var x = point.x
          var y = point.y

          switch orientation {
            case .portrait:
              swap(&x, &y)
            case .landscapeLeft:
              break
            case .portraitUpsideDown:
              x = -x
              y = -y
            case .landscapeRight:
              swap(&x, &y)
              x = -x
              y = -y
            default:
              break
          }

          x *= scaleX
          y *= scaleY

          if autoMode && cameraFacing == .front {
            x = sourceWidth * scaleX - x
          }

          pointsArray.append([
            "x": x,
            "y": y
          ])
        }

        faceContoursTypesMap[faceContoursTypesStrings[i]] = pointsArray
      }
    }

    return faceContoursTypesMap
  }
  
  func getFaceDetector(
    config: [String:Any]
  ) -> FaceDetectorResult {
    var runLandmarks = false
    var runClassifications = false
    var runContours = false
    var trackingEnabled = false
    
    let minFaceSize = 0.15
    let optionsBuilder = FaceDetectorOptions()
        optionsBuilder.performanceMode = .fast
        optionsBuilder.landmarkMode = .none
        optionsBuilder.contourMode = .none
        optionsBuilder.classificationMode = .none
        optionsBuilder.minFaceSize = minFaceSize
        optionsBuilder.isTrackingEnabled = false

    if config["performanceMode"] as? String == "accurate" {
      optionsBuilder.performanceMode = .accurate
    }

    if config["landmarkMode"] as? String == "all" {
      runLandmarks = true
      optionsBuilder.landmarkMode = .all
    }

    if config["classificationMode"] as? String == "all" {
      runClassifications = true
      optionsBuilder.classificationMode = .all
    }

    if config["contourMode"] as? String == "all" {
      runContours = true
      optionsBuilder.contourMode = .all
    }

    let minFaceSizeParam = config["minFaceSize"] as? Double
    if minFaceSizeParam != nil && minFaceSizeParam != minFaceSize {
      optionsBuilder.minFaceSize = CGFloat(minFaceSizeParam!)
    }

    if config["trackingEnabled"] as? Bool == true {
      trackingEnabled = true
      optionsBuilder.isTrackingEnabled = true
    }

    let faceDetector = FaceDetector.faceDetector(
      options: optionsBuilder
    )
  
    return FaceDetectorResult(
      runContours: runContours,
      runClassifications: runClassifications,
      runLandmarks: runLandmarks,
      trackingEnabled: trackingEnabled,
      faceDetector: faceDetector
    )
  }
  
  func processFaces(
    faces: [Face],
    runLandmarks: Bool,
    runClassifications: Bool,
    runContours: Bool,
    trackingEnabled: Bool,
    sourceWidth: CGFloat = 0.0,
    sourceHeight: CGFloat = 0.0,
    scaleX: CGFloat = 1.0,
    scaleY: CGFloat = 1.0,
    autoMode: Bool = false,
    cameraFacing: AVCaptureDevice.Position = .front,
    orientation: UIDeviceOrientation = .portrait
  ) -> [Any] {
    var result: [Any] = []
    
    for face in faces {
      var map: [String: Any] = [:]
      
      if runLandmarks {
        map["landmarks"] = processLandmarks(
          from: face,
          sourceWidth: sourceWidth,
          sourceHeight: sourceHeight,
          scaleX: scaleX,
          scaleY: scaleY,
          autoMode: autoMode
        )
      }
      
      if runClassifications {
        map["leftEyeOpenProbability"] = face.leftEyeOpenProbability
        map["rightEyeOpenProbability"] = face.rightEyeOpenProbability
        map["smilingProbability"] = face.smilingProbability
      }
      
      if runContours {
        map["contours"] = processFaceContours(
          from: face,
          sourceWidth: sourceWidth,
          sourceHeight: sourceHeight,
          scaleX: scaleX,
          scaleY: scaleY,
          autoMode: autoMode,
          cameraFacing: cameraFacing,
          orientation: orientation
        )
      }
      
      if trackingEnabled {
        map["trackingId"] = face.trackingID
      }
      
      map["rollAngle"] = face.headEulerAngleZ
      map["pitchAngle"] = face.headEulerAngleX
      map["yawAngle"] = face.headEulerAngleY
      map["bounds"] = processBoundingBox(
        from: face,
        sourceWidth: sourceWidth,
        sourceHeight: sourceHeight,
        scaleX: scaleX,
        scaleY: scaleY,
        autoMode: autoMode
      )
      
      result.append(map)
    }
    
    return result
  }
}
