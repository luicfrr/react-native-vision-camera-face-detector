## 📚 Introduction

`react-native-vision-camera-face-detector` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using the front camera and visualize the detected faces on the screen.

## 🏗️ Features

- Real-time face detection using front camera
- Integration with Vision Camera library
- Adjustable face visualization with customizable styles
- Base64 frame convertion

## 🧰 Installation

```bash
yarn add react-native-vision-camera-face-detector 
```

## 💡 Usage

OBS 1: You need to add `react-native-worklets-core` plugin to your `babel.config.js`. More details [here](https://react-native-vision-camera.com/docs/guides/frame-processors#react-native-worklets-core).

OBS 2: If you're using `react-native-reanimated` see [this](https://github.com/mrousavy/react-native-vision-camera/issues/1791#issuecomment-1892130378).

OBS 3: Pixel format should be either `yuv` (recomended) or `rgb` (lower performance).

OBS 4: Face bounds are relative to image size not to device screen size so you need to scale it.

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
import { 
  detectFaces,
  DetectionResult 
} from 'react-native-vision-camera-face-detector'
import { Worklets } from 'react-native-worklets-core'

export default function App() {
  const device = useCameraDevice('front')

  useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission()
      console.log({ status })
    })()
  }, [device])

  const handleDetectionWorklet = Worklets.createRunInJsFn( (
    result: DetectionResult
  ) => { 
    console.log( 'detection result', result )
  })
  const frameProcessor = useFrameProcessor((frame) => {
    'worklet'
    runAsync(frame, () => {
      'worklet'
      detectFaces(
        frame,
        handleDetectionWorklet, {
          // detection settings
        }
      )
    })
  }, [handleDetectionWorklet])

  return (
    <View style={{ flex: 1 }}>
      {!!device? <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        frameProcessor={frameProcessor}
        pixelFormat="yuv"
      /> : <Text>
        No Device
      </Text>}
    </View>
  )
}
```

## 👷 Built With

- [React Native](https://reactnative.dev/)
- [Google MLKit](https://developers.google.com/ml-kit)
- [Vision Camera](https://react-native-vision-camera.com/)

## 🔎 About

This package was tested using the following:

- `react-native`: `0.73.2` (new arch disabled)
- `react-native-vision-camera`: `3.8.2`
- `react-native-worklets-core`: `0.3.0`
- `react-native-reanimated`: `3.6.2`
- `expo`: `50.0.4`
- `Android min SDK version`: `26` (Android 8)
- `IOS min version`: `13.4`

If you find any error while using this package you're wellcome to open a new issue or create a new PR.

## 📚 Author

- **Luiz Carlos Ferreira** - [nonam4](https://github.com/nonam4)
