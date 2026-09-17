import AVFoundation
import NitroModules
import Vision
import VisionCamera

class HybridFaceDetectorOutput: HybridCameraOutputSpec, NativeCameraOutput {
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
  var targetResolution: ResolutionRule { .closestTo(Size(width: 720.0, height: 1280.0)) }

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
    super.init()

    self.delegate = FaceDetectorDelegate(onSampleBuffer: { [weak self] buffer, _, _ in
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
    defer { isBusy = false }

    guard let pixelBuffer = CMSampleBufferGetImageBuffer(buffer) else {
      onError(RuntimeError.error(withMessage: "CMSampleBuffer does not contain a CVPixelBuffer!"))
      return
    }

    let frameWidth = Double(CVPixelBufferGetWidth(pixelBuffer))
    let frameHeight = Double(CVPixelBufferGetHeight(pixelBuffer))
    let config = createFaceProcessConfig(
      frameWidth,
      frameHeight,
      autoMode,
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled,
      createOutputToCameraPointTransformer(output: output, frameWidth: frameWidth, frameHeight: frameHeight)
    )

    do {
      let request = VNDetectFaceLandmarksRequest()
      let handler = VNImageRequestHandler(
        cvPixelBuffer: pixelBuffer,
        orientation: outputOrientation.toVisionBufferOrientation(cameraPosition: getCameraPosition()),
        options: [:]
      )
      try handler.perform([request])
      let faces: [any HybridFaceSpec] = (request.results ?? []).map {
        HybridFace(face: $0, config: config)
      }
      onFacesDetected(faces)
    } catch {
      onError(error)
    }
  }

  func configure(config _: OutputConfiguration) {
    output.connection(with: .video)?.preferredVideoStabilizationMode = .off
  }

  private func getCameraPosition() -> AVCaptureDevice.Position {
    if let position = output.connection(with: .video)?
      .inputPorts
      .compactMap({ ($0.input as? AVCaptureDeviceInput)?.device.position })
      .first {
      return position
    }
    switch cameraFacing {
      case .front: return .front
      case .back: return .back
      default: return .unspecified
    }
  }
}
