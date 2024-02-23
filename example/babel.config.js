const path = require( 'path' )
const pak = require( '../package.json' )

module.exports = {
  presets: [ 'babel-preset-expo' ],
  plugins: [
    [ 'react-native-reanimated/plugin', {
      processNestedWorklets: true
    } ],
    [ 'react-native-worklets-core/plugin' ],
    [ 'module-resolver', {
      extensions: [ '.tsx', '.ts', '.js', '.json' ],
      alias: {
        [ pak.name ]: path.join( __dirname, '..', pak.source )
      }
    } ]
  ]
}
