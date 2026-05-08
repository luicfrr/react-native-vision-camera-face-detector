import AVFoundation
import MLKitFaceDetection
import MLKitVision
import NitroModules
import VisionCamera

class HybridFaceDetectorOutput: 
HybridCameraOutputSpec, 
NativeCameraOutput {
  private var delegate: Delegate? = nil
  private let queue: DispatchQueue
  let output = AVCaptureVideoDataOutput()
  let requiresAudioInput: Bool = false
  let requiresDepthFormat: Bool = false
  let mediaType: MediaType = .video
  let streamType: StreamType = .video
  var outputOrientation: CameraOrientation = .up
  var targetResolution: ResolutionRule {
    return .closestTo(
      Size(width: 720.0, height: 1280.0)
    )
  }
  private let orientationManager = FaceDetectorOrientation()
  private let faceDetector: FaceDetector

  init(options: FaceDetectorOutputOptions) {
    self.queue = DispatchQueue(label: "FaceDetectorQueue")
    
    self.faceDetector = FaceDetector.faceDetector(
      options: options.toMLFaceDetectorOptions()
    )

    super.init()

    var isBusy = false
    self.delegate = Delegate(onSampleBuffer: { [weak self] buffer in
      guard let self else { return }
      if isBusy { return }

      isBusy = true
      guard let image = MLImage(sampleBuffer: buffer) else {
        options.onError(
          RuntimeError.error(withMessage: "Failed to convert CMSampleBuffer to MLImage!"))
        return
      }
      image.orientation = self.outputOrientation.toUIImageOrientation()
      let width = image.width
      let height = image.height
      let autoMode = options.autoMode ?? false
      let windowWidth = options.windowWidth ?? 1.0
      let windowHeight = options.windowHeight ?? 1.0 
      let scaleX = autoMode ? windowWidth / width : 1.0
      let scaleY = autoMode ? windowHeight / height : 1.0
      let config = FaceProcessConfig(
        width: width,
        height: height,
        scaleX: scaleX,
        scaleY: scaleY,
        runLandmarks: options.runLandmarks ?? false,
        runContours: options.runContours ?? false,
        runClassifications: options.runClassifications ?? false,
        trackingEnabled: options.trackingEnabled ?? false,
        autoMode: autoMode,
        cameraFacing: options.cameraFacing ?? .front,
        orientation: orientationManager.orientation
      )

      self.faceDetector.process(image) { faces, error in
        isBusy = false
        if let faces {
          let hybridFaces = faces.map { 
            HybridFace(
              face: $0,
              config: config
            ) 
          }
          options.onFacesDetected(hybridFaces)
        }
        if let error {
          options.onError(error)
        }
      }
    })
    self.output.setSampleBufferDelegate(delegate, queue: queue)
    self.output.alwaysDiscardsLateVideoFrames = true
    if #available(iOS 17.0, *), options.outputResolution != .full {
      self.output.automaticallyConfiguresOutputBufferDimensions = false
      self.output.deliversPreviewSizedOutputBuffers = true
    }
  }

  func configure(config: CameraOutputConfiguration) {
    guard let connection = self.output.connection(with: .video) else {
      return
    }
    connection.preferredVideoStabilizationMode = .off
  }

  final class Delegate: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    private let onSampleBuffer: (CMSampleBuffer) -> Void

    init(onSampleBuffer: @escaping (CMSampleBuffer) -> Void) {
      self.onSampleBuffer = onSampleBuffer
      super.init()
    }

    func captureOutput(
      _ output: AVCaptureOutput, 
      didOutput sampleBuffer: CMSampleBuffer,
      from connection: AVCaptureConnection
    ) {
      onSampleBuffer(sampleBuffer)
    }
  }
}
