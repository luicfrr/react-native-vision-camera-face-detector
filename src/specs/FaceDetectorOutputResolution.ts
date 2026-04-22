/**
 * Controls the camera buffer resolution used for face detection.
 *
 * - `'preview'`: Prefer preview-sized buffers for lower latency.
 * - `'full'`: Prefer full/highest available buffers for better detail.
 */
export type FaceDetectorOutputResolution = 'preview' | 'full'
