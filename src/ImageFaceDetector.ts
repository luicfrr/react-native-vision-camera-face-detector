import { Image, NativeModules } from 'react-native'

const MODULE_NAME = 'ImageFaceDetector'

export type InputImage = number | string | { uri: string }

function resolveUri(image: InputImage): string {
  if (typeof image === 'number') {
    const source = Image.resolveAssetSource(image)
    if (!source?.uri) throw new Error('Failed to resolve asset source from number id.')
    return source.uri
  }

  if (typeof image === 'string') {
    return image
  }

  if (typeof image === 'object' && image?.uri) {
    return image.uri
  }

  throw new Error('Unsupported image input. Expected asset id, uri string, or { uri } object.')
}

/**
 * Detect if the given static image contains at least one face.
 *
 * Accepts require('path/to/image.png'), a uri string (file://, content://, http(s)://), or an object { uri }.
 */
export async function hasFace(image: InputImage): Promise<boolean> {
  const uri = resolveUri(image)
  // @ts-ignore
  const NativeModule = NativeModules[MODULE_NAME]
  if (!NativeModule?.hasFaceInImage) {
    throw new Error(`${MODULE_NAME}.hasFaceInImage is not available. Did you rebuild the native app?`)
  }
  const result = await NativeModule.hasFaceInImage(uri)
  return !!result
}

/**
 * Count faces in a static image.
 * Returns the number of detected faces (0 if none).
 */
export async function countFaces(image: InputImage): Promise<number> {
  const uri = resolveUri(image)
  // @ts-ignore
  const NativeModule = NativeModules[MODULE_NAME]
  if (!NativeModule?.countFacesInImage) {
    throw new Error(`${MODULE_NAME}.countFacesInImage is not available. Did you rebuild the native app?`)
  }
  const result = await NativeModule.countFacesInImage(uri)
  return typeof result === 'number' ? result : (result ? 1 : 0)
}
