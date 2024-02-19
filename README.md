## 📚 Introduction

`react-native-vision-camera-face-detector` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using the front camera and visualize the detected faces on the screen.

## 🏗️ Features

- Real-time face detection using front camera
- Integration with Vision Camera library
- Adjustable face visualization with customizable styles
- Base64 frame convertion

## 🧰 Installation

```bash
yarn add react-native-vision-camera-face-detector react-native-worklets-core
```

You need to add `react-native-worklets-core` plugin to your `babel.config.js`. More details [here](https://react-native-vision-camera.com/docs/guides/frame-processors#react-native-worklets-core).

## 💡 Usage

OBS: Face bounds are relative to image size not to device screen size so you need to scale it by multiplying desired bounds data by screen size divided by frame size: `bounds.XX * (deviceWidth|Height / frame.width|height)`.

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

- `react-native`: `0.73.4` (new arch disabled)
- `react-native-vision-camera`: `3.9.0`
- `react-native-worklets-core`: `0.3.0`
- `react-native-reanimated`: `3.7.0`
- `expo`: `50.0.7`

Min Android/IOS versions:

- `Android SDK`: `26` (Android 8)
- `IOS`: `13.4`

If you find any error while using this package you're wellcome to open a new issue or create a new PR.

## 📚 Author

- **Luiz Carlos Ferreira** - [nonam4](https://github.com/nonam4)
