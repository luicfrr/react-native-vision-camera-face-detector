export default {
  expo: {
    newArchEnabled: false,
    name: 'Face Detector Example',
    slug: 'face-detector-example',
    version: '1.0.0',
    jsEngine: 'hermes',
    orientation: 'portrait',
    icon: './assets/icon.png',
    splash: {
      'image': './assets/splash.png',
      'resizeMode': 'contain',
      'backgroundColor': '#ffffff'
    },
    assetBundlePatterns: [
      '**/*'
    ],
    ios: {
      bundleIdentifier: 'com.facedetector.example',
      buildNumber: '1',
      privacyManifests: {
        NSPrivacyAccessedAPITypes: [ {
          NSPrivacyAccessedAPIType: 'NSPrivacyAccessedAPICategoryUserDefaults',
          NSPrivacyAccessedAPITypeReasons: [ 'CA92.1' ]
        } ]
      }
    },
    android: {
      package: 'com.facedetector.example',
      versionCode: 1,
      adaptiveIcon: {
        foregroundImage: './assets/adaptive-icon.png',
        backgroundColor: '#ffffff'
      }
    },
    plugins: [
      [ 'react-native-vision-camera', {
        cameraPermissionText: '$(PRODUCT_NAME) needs to access your device\'s camera.'
      } ],
      [ 'expo-image-picker', {
        photosPermission: 'The app accesses your photos to let you share them with your friends.'
      } ],
      [ 'expo-build-properties', {
        android: {
          // android 8
          minSdkVersion: 26,
          // android 14
          compileSdkVersion: 35,
          targetSdkVersion: 35,
          buildToolsVersion: '35.0.0'
        },
        ios: {
          deploymentTarget: '15.5',
          useFrameworks: 'static'
        }
      } ]
    ]
  }
}
