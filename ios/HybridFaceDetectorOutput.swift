import AVFoundation
import MLKitFaceDetection
import MLKitVision
import NitroModules
import VisionCamera

class HybridFaceDetectorOutput: 
HybridCameraOutputSpec, 
NativeCameraOutput {
  private let queue: DispatchQueue
  private let onFacesDetected: (_ faces: [any HybridFaceSpec]) -> Void
  private let onError: (_ error: Error) -> Void
  private let autoMode: Bool
  private let windowWidth: Double
  private let windowHeight: Double
  private let runLandmarks: Bool
  private let runContours: Bool
  private let runClassifications: Bool
  private let trackingEnabled: Bool
  private let cameraFacing: CameraPosition
  private var delegate: FaceDetectorDelegate? = nil
  private var isBusy = false
  let output: AVCaptureVideoDataOutput
  let requiresAudioInput: Bool = false
  let requiresDepthFormat: Bool = false
  let mediaType: MediaType = .video
  let streamType: StreamType = .video
  var outputOrientation: CameraOrientation = .up
  var currentResolution: Size? {
    guard let connection = output.connection(with: .video) else { return nil }
    return connection.inputStreamResolution
  }
  var targetResolution: ResolutionRule {
    return .closestTo(
      Size(width: 720.0, height: 1280.0)
    )
  }
  private let orientationManager = FaceDetectorOrientation()
  private let faceDetector: FaceDetector

  init(options: FaceDetectorOutputOptions) {
    self.queue = DispatchQueue(label: "FaceDetectorQueue")
    self.output = AVCaptureVideoDataOutput()
    self.onFacesDetected = options.onFacesDetected
    self.onError = options.onError
    self.autoMode = options.autoMode ?? false
    self.windowWidth = options.windowWidth ?? 1.0
    self.windowHeight = options.windowHeight ?? 1.0 
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    self.cameraFacing = options.cameraFacing ?? .front
    self.faceDetector = FaceDetector.faceDetector(
      options: options.toMLFaceDetectorOptions()
    )
    
    super.init()

    self.delegate = FaceDetectorDelegate(onSampleBuffer: { [weak self] buffer in
      self?.detectFaces(buffer)
    })
    self.output.setSampleBufferDelegate(delegate, queue: queue)
    self.output.alwaysDiscardsLateVideoFrames = true
    if #available(iOS 17.0, *), options.outputResolution != .full {
      self.output.automaticallyConfiguresOutputBufferDimensions = false
      self.output.deliversPreviewSizedOutputBuffers = true
    }
  }

  private func detectFaces(_ buffer: CMSampleBuffer) {
    if isBusy { return }

    isBusy = true
    guard let image = MLImage(sampleBuffer: buffer) else {
      isBusy = false
      onError(RuntimeError.error(
        withMessage: "Failed to convert CMSampleBuffer to MLImage!"
      ))
      return
    }
    image.orientation = outputOrientation.toUIImageOrientation(
      orientation: orientationManager.orientation,
      cameraFacing: cameraFacing
    )
    let width = image.height
    let height = image.width
    let config = FaceProcessConfig(
      width: width,
      height: height,
      scaleX: autoMode ? windowWidth / width : 1.0,
      scaleY: autoMode ? windowHeight / height : 1.0,
      runLandmarks: runLandmarks,
      runContours: runContours,
      runClassifications: runClassifications,
      trackingEnabled: trackingEnabled,
      autoMode: autoMode,
      cameraFacing: cameraFacing,
      orientation: orientationManager.orientation
    )

    self.faceDetector.process(image) { [weak self] faces, error in
      guard let self else { return }
      self.isBusy = false
      if let faces {
        let hybridFaces: [any HybridFaceSpec] = faces.map { 
          HybridFace(
            face: $0,
            config: config
          ) 
        }
        self.onFacesDetected(hybridFaces)
      }
      if let error {
        self.onError(error)
      }
    }
  }

  func configure(config: CameraOutputConfiguration) {
    guard let connection = self.output.connection(with: .video) else {
      return
    }
    connection.preferredVideoStabilizationMode = .off
  }
}
