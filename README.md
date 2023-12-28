# vision-camera-face-detector-v3

![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg) [![npm version](https://badge.fury.io/js/vision-camera-face-detector-v3.svg)](https://www.npmjs.com/package/vision-camera-face-detector-v3)

## Description

`react-native-vision-camera-face-detector-v3` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using the front camera and visualize the detected faces on the screen.
This also include a function called frameResize that fixes the mismatch between the frame and view size.

## Features

- Real-time face detection using the front camera
- Integration with the Vision Camera module
- Adjustable face visualization with customizable styles
- **Frame Resize Functionality**: Includes a function called `frameResize()` that fixes the mismatch between the frame and view size, allowing for accurate face visualization on the screen.

## Installation

```bash
yarn add vision-camera-face-detector-v3
```

## Usage

```jsx
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState } from 'react';
import {
  Camera,
  useCameraDevice,
  useFrameProcessor,
} from 'react-native-vision-camera';
import { scanFaces, frameResize } from 'vision-camera-face-detector-v3';
import { Worklets } from 'react-native-worklets-core';

export default function App() {
  const device = useCameraDevice('front');

  React.useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission();
      console.log({ status });
    })();
  }, [device]);

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet';

    try {
      const scannedFaces = scanFaces(frame, {});
      console.log(scannedFaces);
    } catch (error) {
      console.error({ error });
    }
  }, []);

  if (device == null) return <Text>No Device</Text>;
  if (device) {
    return (
      <View style={{ position: 'relative', flex: 1 }}>
        <Camera
          style={StyleSheet.absoluteFill}
          device={device}
          isActive={!!device}
          frameProcessor={frameProcessor}
          //pixel format should be either yuv or rgb
          pixelFormat="yuv"
        />
      </View>
    );
  }
}
```

## Configuration

Ensure that you have the necessary permissions for camera usage. The library includes a function `Camera.requestCameraPermission()` for requesting camera permissions.

- Also make sure that the pixelFormat is not set to `native`, as the frameProccessor needs to be either `yuv` or `rgb`

## Frame Resize Fix

The `frameResize()` function provided by `vision-camera-face-detector-v3` allows you to adjust the detected face coordinates to match the dimensions of your view. This is particularly useful when visualizing the detected faces on the screen. Here's an example of how to use it in conjunction with the React Native Vision Camera:

### Parameters:

- `faces`: An array containing face data obtained from the face detection process.
- `frameHeight`: Height of the camera frame.
- `frameWidth`: Width of the camera frame.
- `viewHeight`: Height of the view where faces will be visualized.
- `viewWidth`: Width of the view where faces will be visualized.

```jsx
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState } from 'react';
import {
  Camera,
  useCameraDevice,
  useFrameProcessor,
} from 'react-native-vision-camera';
import { scanFaces, frameResize } from 'vision-camera-face-detector-v3';
import { Worklets } from 'react-native-worklets-core';

export default function App() {
  const device = useCameraDevice('front');
  const [face, setFace] = useState(null);
  const [fixedFace, setFixedFace] = useState(null);
  const [viewDimensions, setViewDimensions] = useState({ height: 0, width: 0 });
  const [frameDimensions, setFrameDimensions] = useState({
    height: 0,
    width: 0,
  });

  React.useEffect(() => {
    (async () => {
      const status = await Camera.requestCameraPermission();
      console.log({ status });
    })();
  }, [device]);

  const onFaceDetected = Worklets.createRunInJsFn((faces, frameInfo) => {
    setFrameDimensions(frameInfo);
    if (faces.length > 0) {
      setFace(faces);
    } else {
      setFace(null);
    }
  });

  const frameProcessor = useFrameProcessor((frame) => {
    'worklet';

    try {
      const scannedFaces = scanFaces(frame, {});
      if (scannedFaces.length > 0) {
        const { height, width } = frame;
        const frameInfo = { height, width };
        onFaceDetected(scannedFaces, frameInfo);
      }
    } catch (error) {
      console.error({ error });
    }
  }, []);

  useEffect(() => {
    if (face) {
      const fix = frameResize(
        face,
        frameDimensions.height,
        frameDimensions.width,
        viewDimensions.height,
        viewDimensions.width
      );
      setFixedFace(fix);
    }
    return () => {};
  }, [face]);

  if (device == null) return <Text>No Device</Text>;
  if (device) {
    return (
      <View style={{ position: 'relative', flex: 1 }}>
        <Camera
          enableFpsGraph
          onLayout={(e) => {
            setViewDimensions({
              height: e.nativeEvent.layout.height,
              width: e.nativeEvent.layout.width,
            });
          }}
          style={StyleSheet.absoluteFill}
          device={device}
          isActive={!!device}
          frameProcessor={frameProcessor}
          //pixel format should be either yuv or rgb
          pixelFormat="yuv"
        />
        {fixedFace != null && (
          <View
            style={{
              borderColor: 'red',
              borderWidth: 3,
              width: fixedFace.bounds.height,
              height: fixedFace.bounds.width,
              left: fixedFace.bounds.left,
              top: fixedFace.bounds.top,
              position: 'absolute',
              zIndex: 100,
            }}
          />
        )}
      </View>
    );
  }
}
```

## Support and Contribution

If you encounter any issues or have suggestions, feel free to open an issue or submit a pull request.

Certainly! You can add a "Credits" section to your README to acknowledge the repository `vision-camera-face-detector-tmp`. Here's an example:

## Credits

This project utilizes the `vision-camera-face-detector-tmp` repository, which provides essential functionalities for face detection. We extend our gratitude to the contributors and maintainers of `vision-camera-face-detector-tmp` for their valuable work.

- [vision-camera-face-detector-tmp](https://github.com/example-user/vision-camera-face-detector-tmp)

Please check out their repository for additional details and consider giving them a star to show your appreciation.
