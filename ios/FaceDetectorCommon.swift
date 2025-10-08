import MLKitFaceDetection

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

  func processBoundingBox(
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

  func processLandmarks(
    from face: Face,
    scaleX: CGFloat = 1.0,
    scaleY: CGFloat = 1.0
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
      let position = [
        "x": landmark?.position.x ?? 0.0 * scaleX,
        "y": landmark?.position.y ?? 0.0 * scaleY
      ]
      faceLandMarksTypesMap[faceLandmarksTypesStrings[i]] = position
    }

    return faceLandMarksTypesMap
  }

  func processFaceContours(
    from face: Face,
    scaleX: CGFloat = 1.0,
    scaleY: CGFloat = 1.0
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
      let contour = face.contour(ofType: faceContoursTypes[i]);
      var pointsArray: [[String:CGFloat]] = []

      if let points = contour?.points {
        for point in points {
          let currentPointsMap = [
            "x": point.x * scaleX,
            "y": point.y * scaleY,
          ]

          pointsArray.append(currentPointsMap)
        }

        faceContoursTypesMap[faceContoursTypesStrings[i]] = pointsArray
      }
    }

    return faceContoursTypesMap
  }
}
