import React, {
  ReactNode,
  useEffect,
  useRef,
  useState
} from 'react'
import {
  StyleSheet,
  Text,
  Button,
  View,
  useWindowDimensions
} from 'react-native'
import {
  CameraPosition,
  DrawableFrame,
  Frame,
  Camera as VisionCamera,
  useCameraDevice,
  useCameraPermission
} from 'react-native-vision-camera'
import { launchImageLibraryAsync } from 'expo-image-picker'
import { useIsFocused } from '@react-navigation/core'
import { useAppState } from '@react-native-community/hooks'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { NavigationContainer } from '@react-navigation/native'
import {
  Camera,
  Face,
  FaceDetectionOptions,
  Contours,
  Landmarks,
  detectFaces
} from 'react-native-vision-camera-face-detector'
import {
  ClipOp,
  Skia,
  TileMode
} from '@shopify/react-native-skia'
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withTiming
} from 'react-native-reanimated'

/**
 * Entry point component
 *
 * @return {ReactNode} Component
 */
function Index(): ReactNode {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <FaceDetection />
      </NavigationContainer>
    </SafeAreaProvider>
  )
}

/**
 * Face detection component
 *
 * @return {ReactNode} Component
 */
function FaceDetection(): ReactNode {
  const {
    width,
    height
  } = useWindowDimensions()
  const {
    hasPermission,
    requestPermission
  } = useCameraPermission()
  const [
    cameraMounted,
    setCameraMounted
  ] = useState<boolean>( false )
  const [
    cameraPaused,
    setCameraPaused
  ] = useState<boolean>( false )
  const [
    autoMode,
    setAutoMode
  ] = useState<boolean>( true )
  const [
    cameraFacing,
    setCameraFacing
  ] = useState<CameraPosition>( 'front' )
  const faceDetectionOptions = useRef<FaceDetectionOptions>( {
    performanceMode: 'fast',
    classificationMode: 'all',
    contourMode: 'all',
    landmarkMode: 'all',
    windowWidth: width,
    windowHeight: height
  } ).current
  const isFocused = useIsFocused()
  const appState = useAppState()
  const isCameraActive = (
    !cameraPaused &&
    isFocused &&
    appState === 'active'
  )
  const cameraDevice = useCameraDevice( cameraFacing )
  //
  // vision camera ref
  //
  const camera = useRef<VisionCamera>( null )
  //
  // face rectangle position
  //
  const aFaceW = useSharedValue( 0 )
  const aFaceH = useSharedValue( 0 )
  const aFaceX = useSharedValue( 0 )
  const aFaceY = useSharedValue( 0 )
  const aRot = useSharedValue( 0 )
  const boundingBoxStyle = useAnimatedStyle( () => ( {
    position: 'absolute',
    borderWidth: 4,
    borderLeftColor: 'rgb(0,255,0)',
    borderRightColor: 'rgb(0,255,0)',
    borderBottomColor: 'rgb(0,255,0)',
    borderTopColor: 'rgb(255,0,0)',
    width: withTiming( aFaceW.value, {
      duration: 100
    } ),
    height: withTiming( aFaceH.value, {
      duration: 100
    } ),
    left: withTiming( aFaceX.value, {
      duration: 100
    } ),
    top: withTiming( aFaceY.value, {
      duration: 100
    } ),
    transform: [ {
      rotate: `${ aRot.value }deg`
    } ]
  } ) )

  useEffect( () => {
    if ( hasPermission ) return
    requestPermission()
  }, [] )

  /**
   * Handle camera UI rotation
   * 
   * @param {number} rotation Camera rotation
   */
  function handleUiRotation(
    rotation: number
  ) {
    aRot.value = rotation
  }

  /**
   * Hanldes camera mount error event
   *
   * @param {any} error Error event
   */
  function handleCameraMountError(
    error: any
  ) {
    console.error( 'camera mount error', error )
  }

  /**
   * Handle detection result
   * 
   * @param {Face[]} faces Detection result 
   * @param {Frame} frame Current frame
   * @returns {void}
   */
  function handleFacesDetected(
    faces: Face[],
    frame: Frame
  ): void {
    // if no faces are detected we do nothing
    if ( faces.length <= 0 ) {
      aFaceW.value = 0
      aFaceH.value = 0
      aFaceX.value = 0
      aFaceY.value = 0
      return
    }

    console.log(
      'faces', faces.length,
      'frame', frame.toString(),
      'faces', JSON.stringify( faces )
    )

    const { bounds } = faces[ 0 ]
    const {
      width,
      height,
      x,
      y
    } = bounds
    aFaceW.value = width
    aFaceH.value = height
    aFaceX.value = x
    aFaceY.value = y

    // only call camera methods if ref is defined
    if ( camera.current ) {
      // take photo, capture video, etc...
    }
  }

  /**
   * Handle skia frame actions
   * 
   * @param {Face[]} faces Detection result 
   * @param {DrawableFrame} frame Current frame
   * @returns {void}
   */
  function handleSkiaActions(
    faces: Face[],
    frame: DrawableFrame
  ): void {
    'worklet'
    // if no faces are detected we do nothing
    if ( faces.length <= 0 ) return

    console.log(
      'SKIA - faces', faces.length,
      'frame', frame.toString()
    )

    const {
      bounds,
      contours,
      landmarks
    } = faces[ 0 ]

    // draw a blur shape around the face points
    const blurRadius = 25
    const blurFilter = Skia.ImageFilter.MakeBlur(
      blurRadius,
      blurRadius,
      TileMode.Repeat,
      null
    )
    const blurPaint = Skia.Paint()
    blurPaint.setImageFilter( blurFilter )
    const contourPath = Skia.Path.Make()
    const necessaryContours: ( keyof Contours )[] = [
      'FACE',
      'LEFT_CHEEK',
      'RIGHT_CHEEK'
    ]

    necessaryContours.map( ( key ) => {
      contours?.[ key ]?.map( ( point, index ) => {
        if ( index === 0 ) {
          // it's a starting point
          contourPath.moveTo( point.x, point.y )
        } else {
          // it's a continuation
          contourPath.lineTo( point.x, point.y )
        }
      } )
      contourPath.close()
    } )

    frame.save()
    frame.clipPath( contourPath, ClipOp.Intersect, true )
    frame.render( blurPaint )
    frame.restore()

    // draw mouth shape
    const mouthPath = Skia.Path.Make()
    const mouthPaint = Skia.Paint()
    mouthPaint.setColor( Skia.Color( 'red' ) )
    const necessaryLandmarks: ( keyof Landmarks )[] = [
      'MOUTH_BOTTOM',
      'MOUTH_LEFT',
      'MOUTH_RIGHT'
    ]

    necessaryLandmarks.map( ( key, index ) => {
      const point = landmarks?.[ key ]
      if ( !point ) return

      if ( index === 0 ) {
        // it's a starting point
        mouthPath.moveTo( point.x, point.y )
      } else {
        // it's a continuation
        mouthPath.lineTo( point.x, point.y )
      }
    } )
    mouthPath.close()
    frame.drawPath( mouthPath, mouthPaint )

    // draw a rectangle around the face
    const rectPaint = Skia.Paint()
    rectPaint.setColor( Skia.Color( 'blue' ) )
    rectPaint.setStyle( 1 )
    rectPaint.setStrokeWidth( 5 )
    frame.drawRect( bounds, rectPaint )
  }

  /**
   * Detect faces from image
   * 
   * @returns {Promise<void>} Promise
   */
  async function detectFacesFromImage(): Promise<void> {
    // No permissions request is necessary for launching the image library
    let result = await launchImageLibraryAsync( {
      mediaTypes: [ 'images' ],
      allowsEditing: true,
      aspect: [ 4, 3 ],
      quality: 1
    } )

    if ( result.canceled ) return

    const faces = await detectFaces( {
      image: result.assets[ 0 ].uri
    } )
    console.log( 'image detected faces', faces )
  }

  /**
   * Detect faces from photo
   * 
   * @returns {Promise<void>} Promise
   */
  async function detectFacesFromPhoto(): Promise<void> {
    if ( !camera.current ) return
    // take snapshot is faster than take photo 
    // but it does not process captured image
    const { path } = await camera.current?.takeSnapshot()
    const faces = await detectFaces( {
      image: `file://${ path }`
    } )
    console.log( 'photo detected faces', faces )
  }

  return ( <>
    <View
      style={ [
        StyleSheet.absoluteFill, {
          alignItems: 'center',
          justifyContent: 'center'
        }
      ] }
    >
      { hasPermission && cameraDevice ? <>
        { cameraMounted && <>
          <Camera
            // @ts-ignore
            ref={ camera }
            style={ StyleSheet.absoluteFill }
            isActive={ isCameraActive }
            device={ cameraDevice }
            onError={ handleCameraMountError }
            faceDetectionCallback={ handleFacesDetected }
            onUIRotationChanged={ handleUiRotation }
            // @ts-ignore
            skiaActions={ handleSkiaActions }
            faceDetectionOptions={ {
              ...faceDetectionOptions,
              autoMode,
              cameraFacing
            } }
          />

          <Animated.View
            style={ boundingBoxStyle }
          />

          { cameraPaused && <Text
            style={ {
              width: '100%',
              backgroundColor: 'rgb(0,0,255)',
              textAlign: 'center',
              color: 'white'
            } }
          >
            Camera is PAUSED
          </Text> }
        </> }

        { !cameraMounted && <Text
          style={ {
            width: '100%',
            backgroundColor: 'rgb(255,255,0)',
            textAlign: 'center'
          } }
        >
          Camera is NOT mounted
        </Text> }
      </> : <Text
        style={ {
          width: '100%',
          backgroundColor: 'rgb(255,0,0)',
          textAlign: 'center',
          color: 'white'
        } }
      >
        No camera device or permission
      </Text> }
    </View>

    <View
      style={ {
        position: 'absolute',
        bottom: 20,
        left: 0,
        right: 0,
        display: 'flex',
        flexDirection: 'column'
      } }
    >
      <View
        style={ {
          width: '100%',
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-around'
        } }
      >
        <Button
          onPress={ detectFacesFromImage }
          title={ 'Detect from file' }
        />

        <Button
          disabled={ !cameraMounted }
          onPress={ detectFacesFromPhoto }
          title={ 'Detect from photo' }
        />
      </View>

      <View
        style={ {
          width: '100%',
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-around'
        } }
      >
        <Button
          onPress={ () => setCameraFacing( ( current ) => (
            current === 'front' ? 'back' : 'front'
          ) ) }
          title={ 'Toggle Cam' }
        />

        <Button
          onPress={ () => setAutoMode( ( current ) => !current ) }
          title={ `${ autoMode ? 'Disable' : 'Enable' } AutoMode` }
        />
      </View>
      <View
        style={ {
          width: '100%',
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-around'
        } }
      >
        <Button
          onPress={ () => setCameraPaused( ( current ) => !current ) }
          title={ `${ cameraPaused ? 'Resume' : 'Pause' } Cam` }
        />

        <Button
          onPress={ () => setCameraMounted( ( current ) => !current ) }
          title={ `${ cameraMounted ? 'Unmount' : 'Mount' } Cam` }
        />
      </View>
    </View>
  </> )
}

export default Index
