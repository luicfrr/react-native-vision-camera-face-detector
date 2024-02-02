## 📚 Introduction

`react-native-vision-camera-face-detector` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using the front camera and visualize the detected faces on the screen.

## 🏗️ Features

- Real-time face detection using front camera
- Integration with Vision Camera library
- Adjustable face visualization with customizable styles
- **Frame Resize Functionality**: Includes a function called `frameResize()` that fixes the mismatch between the frame and view size, allowing for accurate face visualization on the screen.

## 🧰 Installation

```bash
yarn add react-native-vision-camera-face-detector
```

## 💡 Usage

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
import { detectFaces } from 'react-native-vision-camera-face-detector'
import { Worklets } from 'react-native-worklets-core'

export default function App() {
  const device = useCameraDevice('front')

  useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission()
      console.log({ status })
    })()
  }, [device])

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet'
    runAsync(frame, () => {
      'worklet'
      try {
        const detectionResult = detectFaces(frame)
        console.log('faces detected:', detectionResult.faces)
      } catch (error) {
        console.error(error)
      }
    })
  }, [])

  return (
    <View style={{ flex: 1 }}>
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
## 👷 Built With

- [React Native](https://reactnative.dev/)
- [Google MLKit](https://developers.google.com/ml-kit)
- [Vision Camera](https://react-native-vision-camera.com/)

## 📚 Author

- **Luiz Carlos Ferreira** - [nonam4](https://github.com/nonam4)
