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
      options.contourMode = .all
    }
        
    let minFaceSizeParam = config?["minFaceSize"] as? Double
    if minFaceSizeParam != nil && minFaceSizeParam != minFaceSize {
      options.minFaceSize = CGFloat(minFaceSizeParam!)
    }

    if config?["trackingEnabled"] as? String == "true" {
      options.isTrackingEnabled = true
    }
       
    faceDetector = FaceDetector.faceDetector(options: options)
  }

  func processBoundingBox(from face: Face) -> [String:Any] {
    let boundingBox = face.frame
    let x = boundingBox.origin.x
    let y = boundingBox.origin.y
    let width = boundingBox.width
    let height = boundingBox.height

    return [
      "width": width,
      "height": height,
      "top": y,
      "left": x,
      "right": x + width,
      "bottom": y + height,
      "centerX": boundingBox.midX,
      "centerY": boundingBox.midY
    ]
  }
   
  func processLandmarks(from face: Face) -> [String:[String: CGFloat?]] {
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
        "x": landmark?.position.x,
        "y": landmark?.position.y
      ]
      faceLandMarksTypesMap[faceLandmarksTypesStrings[i]] = position
    }
      
    return faceLandMarksTypesMap
  }
    
  func processFaceContours(from face: Face) -> [String:[[String:CGFloat]]] {
    let faceContoursTypes = [
      FaceContourType.face,
      FaceContourType.leftEyebrowTop,
      FaceContourType.leftEyebrowBottom,
      FaceContourType.rightEyebrowTop,
      FaceContourType.rightEyebrowBottom,
      FaceContourType.leftEye,
      FaceContourType.rightEye,
      FaceContourType.upperLipTop,
      FaceContourType.upperLipBottom,
      FaceContourType.lowerLipTop,
      FaceContourType.lowerLipBottom,
      FaceContourType.noseBridge,
      FaceContourType.noseBottom,
      FaceContourType.leftCheek,
      FaceContourType.rightCheek,
    ]
      
    let faceContoursTypesStrings = [
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
     "RIGHT_CHEEK",
    ];
      
    var faceContoursTypesMap: [String:[[String:CGFloat]]] = [:]
    for i in 0..<faceContoursTypes.count {
      let contour = face.contour(ofType: faceContoursTypes[i]);
      var pointsArray: [[String:CGFloat]] = []
        
      if let points = contour?.points {
        for point in points {
          let currentPointsMap = [
            "x": point.x,
            "y": point.y,
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

  func getOrientationDescription(orientation: UIImage.Orientation) -> String {
    switch orientation {
      case .left, .leftMirrored:
        return "landscape-left"
      case .right, .rightMirrored:
        return "landscape-right"
      case .down, .downMirrored:
        return "portrait-upside-down"
      default:
        return "portrait"
    }
  }
    
  public override func callback(_ frame: Frame, withArguments arguments: [AnyHashable: Any]?) -> Any? {
    let config = getConfig(withArguments: arguments)
    if faceDetector == nil {
      initFD(config: config)
    }

    let image = VisionImage(buffer: frame.buffer)
      image.orientation = frame.orientation
    var result: [String: Any] = [:]
    var faceList: [Any] = []
        
    do {
      let faces: [Face] = try faceDetector.results(in: image)
      if (!faces.isEmpty) {
        for face in faces {
          var map: [String: Any] = [:]
           
          if config?["landmarkMode"] as? String == "all" {
            map["landmarks"] = processLandmarks(from: face)
          }

          if config?["classificationMode"] as? String == "all" {
            map["leftEyeOpenProbability"] = face.leftEyeOpenProbability
            map["rightEyeOpenProbability"] = face.rightEyeOpenProbability
            map["smilingProbability"] = face.smilingProbability
          }

          if config?["contourMode"] as? String == "all" {
            map["contours"] = processFaceContours(from: face)
          }

          if config?["trackingEnabled"] as? String == "true" {
            map["trackingId"] = face.trackingID
          }

          map["rollAngle"] = face.headEulerAngleZ
          map["pitchAngle"] = face.headEulerAngleX
          map["yawAngle"] = face.headEulerAngleY
          map["bounds"] = processBoundingBox(from: face)
                   
          faceList.append(map)
        }
      }

      var frameMap: [String: Any] = [:]
      frameMap["width"] = frame.width
      frameMap["height"] = frame.height
      frameMap["orientation"] = getOrientationDescription(orientation: frame.orientation)
      if config?["convertFrame"] as? String == "true" {
        frameMap["frameData"] = convertFrameToBase64(frame)
      }

      result = [
        "faces": faceList,
        "frame": frameMap
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
