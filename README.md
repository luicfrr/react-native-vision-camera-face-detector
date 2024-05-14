## 📚 Introduction

`react-native-vision-camera-face-detector` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using device's front and back camera.

If you like this package please give it a ⭐ on [GitHub](https://github.com/luicfrr/react-native-vision-camera-face-detector).

## 🏗️ Features

- Real-time face detection using front and back camera
- Adjustable face detection settings
- Optional native side face bounds, contour and landmarks auto scaling
- Can be combined with [Skia Frame Processor](https://react-native-vision-camera.com/docs/guides/skia-frame-processors)

## 🧰 Installation

```bash
yarn add react-native-vision-camera-face-detector
```

Then you need to add `react-native-worklets-core` plugin to `babel.config.js`. More details [here](https://react-native-vision-camera.com/docs/guides/frame-processors#react-native-worklets-core).

## 💡 Usage

Recommended way:
```jsx
import { 
  StyleSheet, 
  Text, 
  View 
} from 'react-native'
import { 
  useEffect, 
  useState,
  useRef
} from 'react'
import {
  Frame,
  useCameraDevice
} from 'react-native-vision-camera'
import {
  Face,
  Camera,
  FaceDetectionOptions
} from 'react-native-vision-camera-face-detector'

export default function App() {
  const faceDetectionOptions = useRef<FaceDetectionOptions>( {
    // detection options
  } ).current

  const device = useCameraDevice('front')

  useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission()
      console.log({ status })
    })()
  }, [device])

  function handleFacesDetection(
    faces: Face[],
    frame: Frame
  ) { 
    console.log(
      'faces', faces.length,
      'frame', frame.toString()
    )
  }

  return (
    <View style={{ flex: 1 }}>
      {!!device? <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        faceDetectionCallback={ handleFacesDetection }
        faceDetectionOptions={ faceDetectionOptions }
      /> : <Text>
        No Device
      </Text>}
    </View>
  )
}
```

Or use it following [vision-camera docs](https://react-native-vision-camera.com/docs/guides/frame-processors-interacting):
```jsx
import { 
  StyleSheet, 
  Text, 
  View 
} from 'react-native'
import { 
  useEffect, 
  useState,
  useRef
} from 'react'
import {
  Camera,
  useCameraDevice,
  useFrameProcessor
} from 'react-native-vision-camera'
import { 
  Face,
  useFaceDetector,
  FaceDetectionOptions
} from 'react-native-vision-camera-face-detector'
import { Worklets } from 'react-native-worklets-core'

export default function App() {
  const faceDetectionOptions = useRef<FaceDetectionOptions>( {
    // detection options
  } ).current

  const device = useCameraDevice('front')
  const { detectFaces } = useFaceDetector( faceDetectionOptions )

  useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission()
      console.log({ status })
    })()
  }, [device])

  const handleDetectedFaces = Worklets.createRunOnJS( (
    faces: Face[]
  ) => { 
    console.log( 'faces detected', faces )
  })

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet'
    const faces = detectFaces(frame)
    // ... chain frame processors
    // ... do something with frame
    handleDetectedFaces(faces)
  }, [handleDetectedFaces])

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

## Face Detection Options

| Option  | Description | Default |
| ------------- | ------------- | ------------- |
| `performanceMode` | Favor speed or accuracy when detecting faces.  | `fast` |
| `landmarkMode` | Whether to attempt to identify facial `landmarks`: eyes, ears, nose, cheeks, mouth, and so on. | `none` |
| `contourMode` | Whether to detect the contours of facial features. Contours are detected for only the most prominent face in an image. | `none` |
| `classificationMode` | Whether or not to classify faces into categories such as 'smiling', and 'eyes open'. | `none` |
| `minFaceSize` | Sets the smallest desired face size, expressed as the ratio of the width of the head to width of the image. | `0.15` |
| `trackingEnabled` | Whether or not to assign faces an ID, which can be used to track faces across images. Note that when contour detection is enabled, only one face is detected, so face tracking doesn't produce useful results. For this reason, and to improve detection speed, don't enable both contour detection and face tracking. | `false` |
| `autoScale` | Should auto scale face bounds, contour and landmarks on native side? If this option is disabled all detection results will be relative to frame coordinates, not to screen/preview. You shouldn't use this option if you want to draw on screen using `Skia Frame Processor`. See [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/30#issuecomment-2058805546) and [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/35) for more details. | `false` |

## 🔧 Troubleshooting

Here is a common issue when trying to use this package and how you can try to fix it:

- `Regular javascript function cannot be shared. Try decorating the function with the 'worklet' keyword...`:
  - If you're using `react-native-reanimated` maybe you're missing [this](https://github.com/mrousavy/react-native-vision-camera/issues/1791#issuecomment-1892130378) step.

If you find other errors while using this package you're wellcome to open a new issue or create a PR with the fix.

## 👷 Built With

- [React Native](https://reactnative.dev/)
- [Google MLKit](https://developers.google.com/ml-kit)
- [Vision Camera](https://react-native-vision-camera.com/)

## 🔎 About

This package was tested using the following:

- `react-native`: `0.73.8` (new arch disabled)
- `react-native-vision-camera`: `4.0.3`
- `react-native-worklets-core`: `1.3.0`
- `react-native-reanimated`: `3.10.0`
- `expo`: `50.0.17`

Min O.S version:

- `Android`: `SDK 26` (Android 8)
- `IOS`: `14`

Make sure to follow tested versions and your device is using the minimum O.S version before opening issues.

## 📚 Author

Made with ❤️ by [luicfrr](https://github.com/luicfrr)
