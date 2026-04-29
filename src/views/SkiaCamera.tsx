import React, {
  useEffect,
  useMemo
} from 'react'
import { SkiaCamera as VisionSkiaCamera } from 'react-native-vision-camera-skia'
import { useAsyncRunner } from 'react-native-vision-camera'
import { createSynchronizable } from 'react-native-worklets'
import useFaceDetector from '../hooks/useFaceDetector'
import useRunInJS from '../hooks/useRunInJs'

// types
import type { Frame } from 'react-native-vision-camera'
import type { SkiaCameraProps } from 'react-native-vision-camera-skia'
import type { Face } from '../specs/Face.nitro'
import type { FaceDetectorOptions } from '../specs/FaceDetectorFactory.nitro'
import type { FaceDetectedCallback } from '../specs/FaceDetectedCallback'

type ComponentType = ( {
  faceDetectorOptions?: FaceDetectorOptions
  faceDetectorCallback: FaceDetectedCallback
  skiaActions?: (
    frame: Frame,
    render: Parameters<SkiaCameraProps[ 'onFrame' ]>[ 1 ],
    faces: Face[]
  ) => void | Promise<void>
} ) & Omit<SkiaCameraProps, 'onFrame'>
/**
 * Vision camera wrapper
 * 
 * @param {ComponentType} props Camera + face detection props 
 * @returns 
 */
export function SkiaCamera( {
  faceDetectorOptions,
  faceDetectorCallback,
  skiaActions,
  ...props
}: ComponentType ) {
  const asyncRunner = useAsyncRunner()
  const faces = createSynchronizable<Face[]>( [] )
  const faceDetector = useFaceDetector( {
    ...faceDetectorOptions,
    autoMode: undefined,
    windowWidth: undefined,
    windowHeight: undefined
  } )

  useEffect( () => {
    return () => faceDetector.stopListeners()
  }, [] )

  /**
   * Runs on detection callback on js thread
   */
  const runOnJs = useRunInJS(
    faceDetectorCallback, [
    faceDetectorCallback
  ] )

  /**
    * Async context that will handle face detection
    */
  const detectFacesAsync = useMemo( () => (
    frame: Frame
  ) => {
    'worklet'

    const finished = asyncRunner.runAsync( () => {
      'worklet'

      try {
        faces.setBlocking(
          faceDetector.detectFaces( frame )
        )
      } catch ( error: any ) {
        console.error(
          'Face detector execution error:',
          error.message ?? JSON.stringify( error )
        )
      } finally {
        frame.dispose()
      }
    } )

    if ( !finished ) {
      frame.dispose()
    }

    runOnJs( faces.getDirty() )
  }, [
    asyncRunner,
    faceDetector,
    runOnJs
  ] )

  return <VisionSkiaCamera
    { ...props }
    pixelFormat='yuv'
    onFrame={ (
      frame,
      render
    ) => {
      'worklet'

      skiaActions?.(
        frame,
        render,
        faces.getDirty()
      )

      detectFacesAsync( frame )
    } }
  />
}

export default SkiaCamera
