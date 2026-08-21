import type { CameraRef, Point } from 'react-native-vision-camera';
import type { Contours } from '../specs/Contours';
import type { Face } from '../specs/Face.nitro';
import type { Landmarks } from '../specs/Landmarks';

type CameraPointToViewConverter = Pick<
  CameraRef,
  'convertCameraPointToViewPoint'
>;

function transformPoint(
  point: Point,
  converter: CameraPointToViewConverter
): Point {
  return converter.convertCameraPointToViewPoint(point);
}

function transformBounds(
  bounds: Face['bounds'],
  converter: CameraPointToViewConverter
): Face['bounds'] {
  const corners = [
    transformPoint({ x: bounds.x, y: bounds.y }, converter),
    transformPoint({ x: bounds.x + bounds.width, y: bounds.y }, converter),
    transformPoint({ x: bounds.x, y: bounds.y + bounds.height }, converter),
    transformPoint(
      { x: bounds.x + bounds.width, y: bounds.y + bounds.height },
      converter
    ),
  ];

  let minX = Number.POSITIVE_INFINITY;
  let minY = Number.POSITIVE_INFINITY;
  let maxX = Number.NEGATIVE_INFINITY;
  let maxY = Number.NEGATIVE_INFINITY;

  for (const corner of corners) {
    minX = Math.min(minX, corner.x);
    minY = Math.min(minY, corner.y);
    maxX = Math.max(maxX, corner.x);
    maxY = Math.max(maxY, corner.y);
  }

  return {
    x: minX,
    y: minY,
    width: maxX - minX,
    height: maxY - minY,
  };
}

function transformLandmarks(
  landmarks: Landmarks | undefined,
  converter: CameraPointToViewConverter
): Landmarks | undefined {
  if (landmarks == null) return undefined;

  const transformed: Landmarks = {};
  for (const key of Object.keys(landmarks) as Array<keyof Landmarks>) {
    const point = landmarks[key];
    if (point != null) {
      transformed[key] = transformPoint(point, converter);
    }
  }
  return transformed;
}

function transformContours(
  contours: Contours | undefined,
  converter: CameraPointToViewConverter
): Contours | undefined {
  if (contours == null) return undefined;

  const transformed: Contours = {};
  for (const key of Object.keys(contours) as Array<keyof Contours>) {
    const points = contours[key];
    if (points != null) {
      transformed[key] = points.map((point) =>
        transformPoint(point, converter)
      );
    }
  }
  return transformed;
}

/**
 * Converts face data from VisionCamera camera coordinates to coordinates
 * relative to a Camera preview view.
 */
export function transformFacesToPreviewCoordinates(
  faces: Face[],
  converter: CameraPointToViewConverter
): Face[] {
  return faces.map(
    (face) =>
      ({
        bounds: transformBounds(face.bounds, converter),
        landmarks: transformLandmarks(face.landmarks, converter),
        contours: transformContours(face.contours, converter),
        leftEyeOpenProbability: face.leftEyeOpenProbability,
        rightEyeOpenProbability: face.rightEyeOpenProbability,
        smilingProbability: face.smilingProbability,
        trackingId: face.trackingId,
        pitchAngle: face.pitchAngle,
        rollAngle: face.rollAngle,
        yawAngle: face.yawAngle,
        frameWidth: face.frameWidth,
        frameHeight: face.frameHeight,
      }) as Face
  );
}
