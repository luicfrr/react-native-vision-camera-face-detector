import React, {
  useEffect
} from 'react'
import { SkiaCamera as VisionSkiaCamera } from 'react-native-vision-camera-skia'
import { useAsyncRunner } from 'react-native-vision-camera'
import { useSharedValue } from 'react-native-reanimated'
import useFaceDetector from '../useFaceDetector'
import useRunInJS from '../hooks/useRunInJs'
import useWorklet from '../hooks/useWorklet'

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
  const faces = useSharedValue<string>( '[]' )
  const {
    detectFaces,
    stopListeners
  } = useFaceDetector( faceDetectorOptions )

  useEffect( () => {
    return () => stopListeners()
  }, [] )

  /** 
     * Throws logs/errors back on js thread
     */
  const logOnJs = useRunInJS( (
    log: string,
    error?: Error
  ) => {
    if ( error ) {
      console.error( log, error.message ?? JSON.stringify( error ) )
    } else {
      console.log( log )
    }
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
  const runOnAsyncContext = useWorklet( (
    frame: Frame
  ) => {
    'worklet'

    const finished = asyncRunner.runAsync( () => {
      'worklet'

      try {
        faces.value = JSON.stringify(
          detectFaces( frame )
        )

        runOnJs(
          JSON.parse( faces.value ),
          frame
        )
      } catch ( error: any ) {
        logOnJs( 'Face detector execution error:', error )
      } finally {
        frame.dispose()
      }
    } )

    if ( !finished ) {
      frame.dispose()
    }
  }, [
    detectFaces,
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
        JSON.parse( faces.value )
      )

      runOnAsyncContext( frame )
      frame.dispose()
    } }
  />
}

export default SkiaCamera
