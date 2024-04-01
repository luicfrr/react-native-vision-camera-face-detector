import VisionCamera
import Foundation
import MLKitFaceDetection
import MLKitVision
import CoreML
import UIKit
import AVFoundation
import SceneKit

@objc(VisionCameraFaceDetector)
public class VisionCameraFaceDetector: FrameProcessorPlugin {
  var context = CIContext(options: nil)
  var faceDetector: FaceDetector! = nil;
  let screenBounds = UIScreen.main.bounds
  
  func initFD(config: [String: Any]!) {
    let minFaceSize = 0.15
    let options = FaceDetectorOptions()
        options.performanceMode = .fast
        options.landmarkMode = .none
        options.contourMode = .none
        options.classificationMode = .none
        options.minFaceSize = minFaceSize
        options.isTrackingEnabled = false

    if config?["performanceMode"] as? String == "accurate" {
      options.performanceMode = .accurate
    }

    if config?["landmarkMode"] as? String == "all" {
      options.landmarkMode = .all
    }

    if config?["contourMode"] as? String == "all" {
      options.contourMode = .all
    }

    if config?["classificationMode"] as? String == "all" {
      options.classificationMode = .all
    }

    let minFaceSizeParam = config?["minFaceSize"] as? Double
    if minFaceSizeParam != nil && minFaceSizeParam != minFaceSize {
      options.minFaceSize = CGFloat(minFaceSizeParam!)
    }

    if config?["trackingEnabled"] as? Bool == true {
      options.isTrackingEnabled = true
    }

    faceDetector = FaceDetector.faceDetector(options: options)
  }

  func processBoundingBox(
    from face: Face,
    scaleX: CGFloat,
    scaleY: CGFloat
  ) -> [String:Any] {
    let boundingBox = face.frame

    return [
      "width": boundingBox.width * scaleX,
      "height": boundingBox.height * scaleY,
      "x": boundingBox.origin.x * scaleX,
      "y": boundingBox.origin.y * scaleY
    ]
  }

  func processLandmarks(
    from face: Face,
    scaleX: CGFloat,
    scaleY: CGFloat
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
    scaleX: CGFloat,
    scaleY: CGFloat
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

  func getConfig(withArguments arguments: [AnyHashable: Any]!) -> [String:Any]! {
    if arguments.count > 0 {
      let config = arguments.map { dictionary in
        Dictionary(uniqueKeysWithValues: dictionary.map { (key, value) in
          (key as? String ?? "", value)
        })
      }

      return config
    }

    return nil
  }

  func convertFrameToBase64(_ frame: Frame) -> Any! {
    guard let imageBuffer = CMSampleBufferGetImageBuffer(frame.buffer) else {
      print("Failed to get CVPixelBuffer!")
      return nil
    }
    let ciImage = CIImage(cvPixelBuffer: imageBuffer)

    guard let cgImage = context.createCGImage(ciImage, from: ciImage.extent) else {
      print("Failed to create CGImage!")
      return nil
    }
    let image = UIImage(cgImage: cgImage)
    let imageData = image.jpegData(compressionQuality: 100)

    return imageData?.base64EncodedString() ?? ""
  }

  public override func callback(_ frame: Frame, withArguments arguments: [AnyHashable: Any]?) -> Any? {
    var result: [String: Any] = [:]

    do {
      let image = VisionImage(buffer: frame.buffer)
      image.orientation = .up

      let scaleX = screenBounds.size.width / CGFloat(frame.width)
      let scaleY = screenBounds.size.height / CGFloat(frame.height)
      let config = getConfig(withArguments: arguments)
      if faceDetector == nil {
        initFD(config: config)
      }

      var faceList: [Any] = []
      let faces: [Face] = try faceDetector.results(in: image)
      for face in faces {
        var map: [String: Any] = [:]

        if config?["landmarkMode"] as? String == "all" {
          map["landmarks"] = processLandmarks(
            from: face,
            scaleX: scaleX,
            scaleY: scaleY
          )
        }

        if config?["classificationMode"] as? String == "all" {
          map["leftEyeOpenProbability"] = face.leftEyeOpenProbability
          map["rightEyeOpenProbability"] = face.rightEyeOpenProbability
          map["smilingProbability"] = face.smilingProbability
        }

        if config?["contourMode"] as? String == "all" {
          map["contours"] = processFaceContours(
            from: face,
            scaleX: scaleX,
            scaleY: scaleY
          )
        }

        if config?["trackingEnabled"] as? Bool == true {
          map["trackingId"] = face.trackingID
        }

        map["rollAngle"] = face.headEulerAngleZ
        map["pitchAngle"] = face.headEulerAngleX
        map["yawAngle"] = face.headEulerAngleY
        map["bounds"] = processBoundingBox(
          from: face,
          scaleX: scaleX,
          scaleY: scaleY
        )

        faceList.append(map)
      }

      var frameMap: [String: Any] = [:]
      let returnOriginal = config?["returnOriginal"] as? Bool == true
      let convertFrame = config?["convertFrame"] as? Bool == true

      if returnOriginal {
        frameMap["original"] = frame
      }

      if  convertFrame {
        frameMap["converted"] = convertFrameToBase64(frame)
      }

      result = returnOriginal || convertFrame ? [
        "faces": faceList,
        "frame": frameMap
      ] : [
        "faces": faceList
      ]
    } catch let error {
      print("Error processing face detection: \(error)")
    }

    return result
  }

  public override init(proxy: VisionCameraProxyHolder, options: [AnyHashable : Any]! = [:]) {
    super.init(proxy: proxy, options: options)
  }
}
