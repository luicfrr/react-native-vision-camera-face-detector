# vision-camera-trustee-face-detector-v3

![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg) [![npm version](https://badge.fury.io/js/vision-camera-trustee-face-detector-v3.svg)](https://www.npmjs.com/package/vision-camera-trustee-face-detector-v3)

## Description

`vision-camera-trustee-face-detector-v3` is a React Native library that integrates with the Vision Camera module to provide face detection functionality. It allows you to easily detect faces in real-time using the front camera and visualize the detected faces on the screen.

## Features

- Real-time face detection using the front camera
- Integration with the Vision Camera module
- Adjustable face visualization with customizable styles
- Convert frame to base64

## Installation

```bash
yarn add vision-camera-trustee-face-detector-v3
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
import { scanFaces } from 'vision-camera-trustee-face-detector-v3';
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
      console.log(scannedFaces?.faces);
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
