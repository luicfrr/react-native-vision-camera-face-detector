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
  private let mirrorMode: MirrorMode
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

  init(options: FaceDetectorOutputOptions) {
    self.queue = DispatchQueue(label: "FaceDetectorQueue")
    self.output = AVCaptureVideoDataOutput()
    self.onFacesDetected = options.onFacesDetected
    self.onError = options.onError
    self.autoMode = options.autoMode ?? false
    self.runLandmarks = options.runLandmarks ?? false
    self.runContours = options.runContours ?? false
    self.runClassifications = options.runClassifications ?? false
    self.trackingEnabled = options.trackingEnabled ?? false
    self.mirrorMode = options.mirrorMode ?? .auto
    self.faceDetector = FaceDetector.faceDetector(
      options: options.toMLFaceDetectorOptions()
    )
    
    super.init()

    self.delegate = FaceDetectorDelegate(onSampleBuffer: { [weak self] buffer, bufferOrientation, isMirrored in
      self?.detectFaces(
        buffer,
        bufferOrientation,
        isMirrored
      )
    })
    self.output.setSampleBufferDelegate(delegate, queue: queue)
    self.output.alwaysDiscardsLateVideoFrames = true
    if #available(iOS 17.0, *), options.outputResolution != .full {
      self.output.automaticallyConfiguresOutputBufferDimensions = false
      self.output.deliversPreviewSizedOutputBuffers = true
    }
  }

  private func detectFaces(
    _ buffer: CMSampleBuffer,
    _ bufferOrientation: CameraOrientation,
    _ isBufferMirrored: Bool
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
    // ML Kit returns Face coordinates in this unrotated buffer's pixel space.
    let frameWidth = Double(CVPixelBufferGetWidth(pixelBuffer))
    let frameHeight = Double(CVPixelBufferGetHeight(pixelBuffer))
    let relativeOrientation = bufferOrientation.relativeToOutput(outputOrientation)
    let imageOrientation = relativeOrientation.toMLKitImageOrientation(
      isMirrored: getRelativeMirrorTransform(isBufferMirrored)
    )
    image.orientation = imageOrientation
    let config = createFaceProcessConfig(
      frameWidth,
      frameHeight,
      autoMode,
      createFrameToCameraPointTransformer(
        frameWidth: frameWidth,
        frameHeight: frameHeight,
        orientation: imageOrientation
      ),
      runLandmarks,
      runContours,
      runClassifications,
      trackingEnabled
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
    // Keep the analysis buffer aligned with the preview's unstabilized camera
    // coordinates. Mirroring stays relative metadata, like VisionCamera's
    // native FrameOutput, and must not be applied physically here.
    output.connection(with: .video)?.preferredVideoStabilizationMode = .off
  }

  private func getRelativeMirrorTransform(
    _ isBufferMirrored: Bool
  ) -> Bool {
    switch mirrorMode {
      // The camera coordinates already follow the connection's physical mirror
      // state in auto mode, so no counter-mirror is needed.
      case .auto: return false
      // Mirror mode is relative to the buffer: only counter-mirror when the
      // buffer does not already have the requested state.
      case .on: return isBufferMirrored == false
      case .off: return isBufferMirrored == true
    }
  }
}
