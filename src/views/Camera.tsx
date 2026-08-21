import React, { forwardRef, useCallback, useRef } from 'react';
import { Camera as VisionCamera } from 'react-native-vision-camera';

// types
import type { CameraViewProps, CameraRef } from 'react-native-vision-camera';
import type { FaceDetectorOutputOptions } from '../specs/FaceDetectorFactory.nitro';
import useFaceDetectorOutput from '../hooks/useFaceDetectorOutput';
import { transformFacesToPreviewCoordinates } from '../utils/transformFacesToPreviewCoordinates';

interface ComponentType
  extends Omit<CameraViewProps, 'onError'>, FaceDetectorOutputOptions {}

/**
 * A view that detects {@linkcode Face}s in a Camera
 * using the default front {@linkcode CameraDevice}.
 *
 *
 * @example
 * ```tsx
 * function App() {
 *   const isFocused = useIsFocused()
 *   const appState = useAppState()
 *   const isActive = isFocused && appState === 'active'
 *   return (
 *     <Camera
 *       isActive={isActive}
 *       barcodeFormats={['all']}
 *       onFacesDetected={(faces) => {
 *         console.log(`Detected ${faces.length} faces!`)
 *       }}
 *       onError={(error) => {
 *         console.error(`Error detecting faces:`, error)
 *       }}
 *     />
 *   )
 * }
 * ```
 */
export const Camera = forwardRef<CameraRef, ComponentType>(function Camera(
  {
    onFacesDetected,
    onError,
    outputResolution,
    cameraFacing,
    mirrorMode,
    resizeMode,
    autoMode,
    windowWidth,
    windowHeight,
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled,
    outputs,
    ...cameraProps
  },
  ref
) {
  const cameraRef = useRef<CameraRef | null>(null);

  const setCameraRef = useCallback(
    (camera: CameraRef | null) => {
      cameraRef.current = camera;

      if (typeof ref === 'function') {
        ref(camera);
      } else if (ref != null) {
        ref.current = camera;
      }
    },
    [ref]
  );

  const handleFacesDetected = useCallback(
    (faces: Parameters<typeof onFacesDetected>[0]) => {
      if (!autoMode || faces.length === 0) {
        onFacesDetected(faces);
        return;
      }

      const camera = cameraRef.current;
      if (camera?.preview == null) return;

      let previewFaces: Parameters<typeof onFacesDetected>[0];
      try {
        previewFaces = transformFacesToPreviewCoordinates(faces, camera);
      } catch {
        // The preview can be unmounted between the readiness check and conversion.
        return;
      }

      onFacesDetected(previewFaces);
    },
    [autoMode, onFacesDetected]
  );

  const output = useFaceDetectorOutput({
    onFacesDetected: handleFacesDetected,
    onError,
    outputResolution,
    cameraFacing,
    mirrorMode,
    autoMode,
    windowWidth,
    windowHeight,
    performanceMode,
    runLandmarks,
    runContours,
    runClassifications,
    minFaceSize,
    trackingEnabled,
  });

  return (
    <VisionCamera
      {...cameraProps}
      ref={setCameraRef}
      mirrorMode={mirrorMode}
      resizeMode={resizeMode}
      outputs={[output, ...(outputs ?? [])]}
      onError={onError}
    />
  );
});

export default Camera;
