import { useEffect } from 'react'
import {
  StyleSheet,
  Text,
  useWindowDimensions,
  Platform
} from 'react-native'
import {
  useCameraDevice,
  useCameraPermission,
} from 'react-native-vision-camera'
import { useIsFocused } from '@react-navigation/core'
import { useAppState } from '@react-native-community/hooks'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { NavigationContainer } from '@react-navigation/native'
import {
  Bounds,
  Camera,
  DetectionResult,
  FrameData
} from 'react-native-vision-camera-face-detector'
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withTiming
} from 'react-native-reanimated'
import { Worklets } from 'react-native-worklets-core'

type FacePosType = {
  faceW: number
  faceH: number
  faceX: number
  faceY: number
}

/**
 * Entry point component
 *
 * @return {JSX.Element} Component
 */
function Index(): JSX.Element {
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
 * @return {JSX.Element} Component
 */
function FaceDetection(): JSX.Element {
  const {
    width: windowWidth,
    height: windowHeight
  } = useWindowDimensions()
  const {
    hasPermission,
    requestPermission
  } = useCameraPermission()
  const isIos = Platform.OS === 'ios'
  const isFocused = useIsFocused()
  const appState = useAppState()
  const isCameraActive = (
    isFocused &&
    appState === 'active'
  )
  const cameraDevice = useCameraDevice( 'front' )
  /**
   * face rectangle position
   */
  const aFaceW = useSharedValue( 0 )
  const aFaceH = useSharedValue( 0 )
  const aFaceX = useSharedValue( 0 )
  const aFaceY = useSharedValue( 0 )
  const animatedStyle = useAnimatedStyle( () => ( {
    position: 'absolute',
    borderWidth: 4,
    borderColor: 'rgb(0,255,0)',
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
    } )
  } ) )

  const handleFacesDetected = Worklets.createRunInJsFn( ( {
    faces,
    frame
  }: DetectionResult ) => {
    // if no faces are detected
    if ( Object.keys( faces ).length <= 0 ) return
    const { bounds } = faces[ 0 ]
    const {
      faceW,
      faceH,
      faceX,
      faceY
    } = calcFacePosition( bounds, frame )
    aFaceW.value = faceW
    aFaceH.value = faceH
    aFaceX.value = faceX
    aFaceY.value = faceY
  } )

  useEffect( () => {
    if ( hasPermission ) return
    requestPermission()
  }, [] )

  /**
   * Calculate face position in screen
   *
   * @param {Bounds} bounds Face detection bounds
   * @param {FrameData} frame Current frame data
   * @return {FacePosType} Face position
   */
  function calcFacePosition(
    bounds: Bounds,
    frame: FrameData
  ): FacePosType {
    const orientation = ( () => {
      switch ( frame.orientation ) {
        case 'portrait': return 0
        case 'landscape-left': return 90
        case 'portrait-upside-down': return 180
        case 'landscape-right': return 270
      }
    } )()
    const degrees = ( orientation - 90 + 360 ) % 360
    let scaleX = 0
    let scaleY = 0

    if ( !isIos && (
      degrees === 90 ||
      degrees === 270
    ) ) {
      // frame sizes are inverted due to vision camera orientation bug
      scaleX = windowWidth / frame.height
      scaleY = windowHeight / frame.width
    } else {
      scaleX = windowWidth / frame.width
      scaleY = windowHeight / frame.height
    }

    const faceW = bounds.width * scaleX
    const faceH = bounds.height * scaleY
    const faceX = ( () => {
      const xPos = bounds.left * scaleX
      if ( isIos ) return xPos
      // invert X position on android
      return windowWidth - ( xPos + faceW )
    } )()

    return {
      faceW,
      faceH,
      // get horizontally face center
      faceX,
      // get vertically face center
      faceY: bounds.top * scaleY
    }
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

  return ( <>
    { hasPermission && cameraDevice ? <>
      <Camera
        style={ StyleSheet.absoluteFill }
        isActive={ isCameraActive }
        device={ cameraDevice }
        onError={ handleCameraMountError }
        faceDetectionCallback={ handleFacesDetected }
        faceDetectionOptions={ {
          performanceMode: 'fast',
          classificationMode: 'all'
        } }
      />
      <Animated.View
        style={ animatedStyle }
      />
    </> : <Text
      style={ {
        backgroundColor: 'rgb(255,0,0)',
        color: 'white'
      } }
    >
      No camera device or permission
    </Text> }
  </> )
}

export default Index
