# react-native-vision-camera-face-detector

![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg) [![npm version](https://badge.fury.io/js/react-native-vision-camera-face-detector.svg)](https://www.npmjs.com/package/react-native-vision-camera-face-detector)

## Description

`react-native-vision-camera-face-detector` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using the front camera and visualize the detected faces on the screen.

## Features

- Real-time face detection using the front camera
- Integration with the Vision Camera module
- Adjustable face visualization with customizable styles
- Convert frame to base64

## Installation

```bash
yarn add react-native-vision-camera-face-detector
```

## Usage

OBS: Pixel format should be either `yuv` (recomended) or `rgb` (lower performance).

```jsx
import { 
  StyleSheet, 
  Text, 
  View 
} from 'react-native'
import { 
  useEffect, 
  useState 
} from 'react'
import {
  Camera,
  useCameraDevice,
  useFrameProcessor
} from 'react-native-vision-camera'
import { scanFaces } from 'react-native-vision-camera-face-detector'
import { Worklets } from 'react-native-worklets-core'

export default function App() {
  const device = useCameraDevice('front')

  React.useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission()
      console.log({ status })
    })()
  }, [device])

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet';

    try {
      const scannedFaces = scanFaces(frame, {})
      console.log(scannedFaces?.faces)
    } catch (error) {
      console.error({ error })
    }
  }, [])

  return (
    <View style={{ 
      position: 'relative', 
      flex: 1
    }}>
      {device? <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={!!device}
        frameProcessor={frameProcessor}
        pixelFormat="yuv"
      /> : <Text>
        No Device
      </Text>}
    </View>
  )
}
```
