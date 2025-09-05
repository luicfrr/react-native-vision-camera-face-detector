#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(ImageFaceDetector, NSObject)
RCT_EXTERN_METHOD(hasFaceInImage:(NSString *)uri resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
RCT_EXTERN_METHOD(countFacesInImage:(NSString *)uri resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
@end
