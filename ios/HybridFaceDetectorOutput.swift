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
  private let faceDetector: FaceDetector

  init(_ options: FaceDetectorOutputOptions) {
    self.queue = DispatchQueue(label: "FaceDetectorQueue")
    self.output = AVCaptureVideoDataOutput()
    self.onFacesDetected = options.onFacesDetected
    self.onError = options.onError
    self.autoMode = options.autoMode ?? false
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    self.cameraFacing = options.cameraFacing ?? .front
    self.faceDetector = FaceDetector.faceDetector(
      options: options.toMLFaceDetectorOptions()
    )
    
    super.init()

    self.delegate = FaceDetectorDelegate(onSampleBuffer: {
      [weak self] buffer, _, _ in
        self?.detectFaces(buffer)
    })
    self.output.setSampleBufferDelegate(delegate, queue: queue)
    self.output.alwaysDiscardsLateVideoFrames = true
    if #available(iOS 17.0, *), options.outputResolution != .full {
      self.output.automaticallyConfiguresOutputBufferDimensions = false
      self.output.deliversPreviewSizedOutputBuffers = true
    }
  }

  private func detectFaces(
    _ buffer: CMSampleBuffer
  ) {
    if isBusy { return }

    isBusy = true
    guard let image = MLImage(sampleBuffer: buffer) else {
      isBusy = false
      onError(RuntimeError.error(
        withMessage: "Failed to convert CMSampleBuffer to MLImage!"
      ))
      return
    }
    guard let pixelBuffer = CMSampleBufferGetImageBuffer(buffer) else {
      isBusy = false
      onError(RuntimeError.error(
        withMessage: "CMSampleBuffer does not contain a CVPixelBuffer!"
      ))
      return
    }
    // ML Kit returns Face coordinates in this raw output buffer's pixel space.
    let frameWidth = Double(CVPixelBufferGetWidth(pixelBuffer))
    let frameHeight = Double(CVPixelBufferGetHeight(pixelBuffer))
    image.orientation = outputOrientation.toMLKitBufferOrientation(
      cameraPosition: getCameraPosition()
    )
    let config = createFaceProcessConfig(
      frameWidth,
      frameHeight,
      autoMode,
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled,
      createOutputToCameraPointTransformer(
        output: output,
        frameWidth: frameWidth,
        frameHeight: frameHeight
      )
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

  func configure(config _: OutputConfiguration) {
    // Keep source buffers unrotated. ML Kit receives the physical orientation
    // as image metadata, and AVFoundation converts its raw result points to
    // the camera coordinate system.
    output.connection(with: .video)?.preferredVideoStabilizationMode = .off
  }

  private func getCameraPosition() -> AVCaptureDevice.Position {
    if let cameraPosition = output.connection(with: .video)?
      .inputPorts
      .compactMap({ ($0.input as? AVCaptureDeviceInput)?.device.position })
      .first
    {
      return cameraPosition
    }

    switch cameraFacing {
      case .front: return .front
      case .back: return .back
      default: return .unspecified
    }
  }
}
