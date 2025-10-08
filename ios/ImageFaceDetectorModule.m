#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(ImageFaceDetector, NSObject)
RCT_EXTERN_METHOD(
  detectFaces:(NSString *)uri 
  options:(NSDictionary *)options
  resolver:(RCTPromiseResolveBlock)resolve 
  rejecter:(RCTPromiseRejectBlock)reject
)
@end
