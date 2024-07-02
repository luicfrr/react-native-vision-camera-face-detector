export default {
  expo: {
    name: 'Face Detector Example',
    slug: "face-detector-example",
    version: '1.0.0',
    jsEngine: 'hermes',
    orientation: "portrait",
    icon: "./assets/icon.png",
    splash: {
      "image": "./assets/splash.png",
      "resizeMode": "contain",
      "backgroundColor": "#ffffff"
    },
    assetBundlePatterns: [
      "**/*"
    ],
    ios: {
      bundleIdentifier: 'com.facedetector.example',
      buildNumber: '1',
      privacyManifests: {
        NSPrivacyAccessedAPITypes: [ {
          NSPrivacyAccessedAPIType: "NSPrivacyAccessedAPICategoryUserDefaults",
          NSPrivacyAccessedAPITypeReasons: [ "CA92.1" ]
        } ]
      }
    },
    android: {
      package: 'com.facedetector.example',
      versionCode: 1,
      adaptiveIcon: {
        foregroundImage: "./assets/adaptive-icon.png",
        backgroundColor: "#ffffff"
      }
    },
    plugins: [
      [ 'react-native-vision-camera', {
        "cameraPermissionText": '$(PRODUCT_NAME) needs to access your device\'s camera.'
      } ],
      [ 'expo-build-properties', {
        'android': {
          // android 8
          minSdkVersion: 26,
          // android 14
          compileSdkVersion: 34,
          targetSdkVersion: 34,
          buildToolsVersion: '34.0.0'
        },
        ios: {
          deploymentTarget: '14.0',
          useFrameworks: 'static'
        }
      } ]
    ]
  }
}
