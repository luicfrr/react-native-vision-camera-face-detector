module.exports = {
  presets: [ 'babel-preset-expo' ],
  plugins: [ [
    'module-resolver', {
      alias: {
        'react-native-vision-camera-face-detector': '../src/index'
      },
      root: [ './src' ],
      'extensions': [
        '.tsx',
        '.ts',
        '.js',
        '.json'
      ]
    } ], [
    'react-native-reanimated/plugin', {
      processNestedWorklets: true
    }
  ], [
    'react-native-worklets-core/plugin'
  ] ]
}
