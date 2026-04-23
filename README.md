## 📚 Introduction

`react-native-vision-camera-face-detector` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using device's front/back camera. Also supports static image face detections.

Is this package usefull to you?

<a href="https://www.buymeacoffee.com/luicfrr" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>

Or give it a ⭐ on [GitHub](https://github.com/luicfrr/react-native-vision-camera-face-detector).

## 🏗️ Features

- Real-time face detection using front and back camera
- Adjustable face detection settings
- Optional native side face bounds, contour and landmarks auto scaling
- Can be combined with [Skia Frame Processor](https://visioncamera.margelo.com/docs/skia-frame-processors)

## 🧰 Installation

```bash
yarn add react-native-vision-camera-face-detector
```

Then you need to add `react-native-worklets` plugin to `babel.config.js`. More details [here](https://docs.swmansion.com/react-native-worklets/docs/#react-native-community-cli).

## 🪲 Knowing Bugs

There's no knowing bugs at this momment...

## 💡 Usage

Recommended way (see [Example App](https://github.com/luicfrr/react-native-vision-camera-face-detector/blob/main/example/src/index.tsx) for Skia usage):
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
  FaceDetectorOptions
} from 'react-native-vision-camera-face-detector'

export default function App() {
  const faceDetectorOptions = useRef<FaceDetectorOptions>( {
    // detector options
  } ).current

  const device = useCameraDevice('front')

  useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission()
      console.log({ status })
    })()
  }, [device])

  function handleFacesDetected(
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
        faceDetectorCallback={ handleFacesDetected }
        faceDetectorOptions={ faceDetectorOptions }
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
  View,
  NativeModules,
  Platform
} from 'react-native'
import { 
  useEffect, 
  useState,
  useRef
} from 'react'
import {
  Camera,
  runAsync,
  useCameraDevice,
  useFrameProcessor
} from 'react-native-vision-camera'
import { 
  Face,
  useFaceDetector,
  FaceDetectorOptions
} from 'react-native-vision-camera-face-detector'
import { Worklets } from 'react-native-worklets-core'

export default function App() {
  const faceDetectorOptions = useRef<FaceDetectorOptions>( {
    // detection options
  } ).current

  const device = useCameraDevice('front')
  const { 
    detectFaces,
    stopListeners
  } = useFaceDetector( faceDetectorOptions )

  const asyncRunner = useAsyncRunner()
  const frameOutput = useFrameOutput({
    onFrame: (frame) => {
      'worklet'
      
      const wasHandled = asyncRunner.runAsync(() => {
        'worklet'
        const faces = detectFaces(frame)
        // ... do something with faces
        // ... chain something asynchronously

        // async task finished - dispose the Frame now.
        frame.dispose()
      })
      
      if (!wasHandled) {
        // `asyncRunner` is busy - drop this Frame!
        frame.dispose()
      } 
    }
  })

  useEffect( () => {
    return () => {
      // you must call `stopListeners` when current component is unmounted
      stopListeners()
    }
  }, [] )

  useEffect(() => {
    if(!device) {
      // you must call `stopListeners` when `Camera` component is unmounted
      stopListeners()
      return
    }

    (async () => {
      const status = await Camera.requestCameraPermission()
      console.log({ status })
    })()
  }, [device])

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet'
    runAsync(frame, () => {
      'worklet'
      
    })
    // ... chain frame processors
    // ... do something with frame
  }, [handleDetectedFaces])

  return (
    <View style={{ flex: 1 }}>
      {!!device? <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={true}
        outputs={[frameOutput]}
      /> : <Text>
        No Device
      </Text>}
    </View>
  )
}
```

As face detection is a heavy process you should run it in an asynchronously so it can be finished without blocking your camera preview.
You should read `vision-camera` [docs](https://visioncamera.margelo.com/docs/async-frame-processing#the-async-runner) about this feature.

## 🖼️ Static Image Face Detection

You can detect faces in static images without the camera (picking images from your gallery/files) or you can use it to detect faces in photos taken from camera (see [Example App](https://github.com/luicfrr/react-native-vision-camera-face-detector/blob/main/example/src/index.tsx)):

Supported image sources: 
- Requirings (`require('path/to/file')`)
- URI string (`file://`, `content://`, `http(s)://`)
- Object (`{ uri: string }`)

```ts
import { useImageFaceDetector } from 'react-native-vision-camera-face-detector'

const { detectFaces } = useImageFaceDetector( {
  // detection options
} )

// Using a bundled asset
const faces1 = await detectFaces(
  require('./assets/photo.jpg')
)
// Using a local file path or content URI (e.g. from an image picker)
const faces2 = await detectFaces(
  'file:///storage/emulated/0/Download/pic.jpg'
)
const faces3 = await detectFaces({ 
  uri: 'content://media/external/images/media/12345' 
})

console.log({ 
  faces1, 
  faces2, 
  faces3 
})
```

## Face Detection Options

#### Image Face Detector
| Option  | Description | Default | Options |
| ------------- | ------------- | ------------- | ------------- |
| `performanceMode` | Favor speed or accuracy when detecting faces.  | `fast` | `fast`, `accurate`|
| `runLandmarks` | Whether to attempt to identify facial `landmarks`: eyes, ears, nose, cheeks, mouth, and so on. | `false` | `boolean` |
| `runContours` | Whether to detect the contours of facial features. Contours are detected for only the most prominent face in an image. | `false` | `boolean` |
| `runClassifications` | Whether or not to classify faces into categories such as 'smiling', and 'eyes open'. | `false` | `boolean` |
| `minFaceSize` | Sets the smallest desired face size, expressed as the ratio of the width of the head to width of the image. | `0.15` | `number` |
| `trackingEnabled` | Whether or not to assign faces an ID, which can be used to track faces across images. Note that when contour detection is enabled, only one face is detected, so face tracking doesn't produce useful results. For this reason, and to improve detection speed, don't enable both contour detection and face tracking. | `false` | `boolean` |


#### Frame Face Detector (extends Image Face Detector)
| Option  | Description | Default | Options |
| ------------- | ------------- | ------------- | ------------- |
| `cameraFacing` | Current active camera | `front` | `front`, `back` |
| `autoMode` | Should handle auto scale (face bounds, contour and landmarks) and rotation on native side? If this option is disabled all detection results will be relative to frame coordinates, not to screen/preview. You should NOT use this option if you want to draw on screen using `Skia Frame Processor`. See [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/30#issuecomment-2058805546) and [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/35) for more details. | `false` | `boolean` |
| `windowWidth` | * Required if you want to use `autoMode`. You must handle your own logic to get screen sizes, with or without statusbar size, etc... | `1.0` | `number` |
| `windowHeight` | * Required if you want to use `autoMode`. You must handle your own logic to get screen sizes, with or without statusbar size, etc... | `1.0` | `number` |

## 🔧 Troubleshooting

Here is a common issue when trying to use this package and how you can try to fix it:

- `Regular javascript function cannot be shared. Try decorating the function with the 'worklet' keyword...`:
  - If you're using `react-native-reanimated` maybe you're missing [this](https://github.com/mrousavy/react-native-vision-camera/issues/1791#issuecomment-1892130378) step.
- `Execution failed for task ':react-native-vision-camera-face-detector:compileDebugKotlin'...`:
  - This error is probably related to gradle cache. Try [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/71#issuecomment-2186614831) sollution first.
  - Also check [this](https://github.com/luicfrr/react-native-vision-camera-face-detector/issues/90#issuecomment-2358160166) comment.

If you find other errors while using this package you're wellcome to open a new issue or create a PR with the fix.

## 👷 Built With

- [React Native](https://reactnative.dev/)
- [Google MLKit](https://developers.google.com/ml-kit)
- [Vision Camera](https://react-native-vision-camera.com/)

## 🔎 About

This package was tested using the following:

- `@react-native-firebase`: `24.0.0`
- `@shopify/react-native-skia`: `2.6.2`
- `react-native`: `0.85.2`
- `react-native-nitro-image`: `0.13.1`
- `react-native-nitro-modules`: `0.35.5`
- `react-native-reanimated`: `4.3.0`
- `react-native-vision-camera`: `5.0.6`
- `react-native-vision-camera-skia`: `5.0.6`
- `react-native-vision-camera-worklets`: `5.0.6`
- `react-native-worklets`: `0.8.1`
- `expo`: `55`

Min O.S version:

- `Android`: `SDK 26` (Android 8)
- `IOS`: `15.5`

Make sure to follow tested versions and your device is using the minimum O.S version before opening issues.

## 📚 Author

Made with ❤️ by [luicfrr](https://github.com/luicfrr)
