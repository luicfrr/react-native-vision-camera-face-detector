#import <Foundation/Foundation.h>
#import <VisionCamera/FrameProcessorPlugin.h>
#import <VisionCamera/FrameProcessorPluginRegistry.h>
#import <VisionCamera/Frame.h>

#if __has_include("VisionCameraFaceDetector/VisionCameraFaceDetector-Swift.h")
#import "VisionCameraFaceDetector/VisionCameraFaceDetector-Swift.h"
#else
#import "VisionCameraFaceDetector-Swift.h"
#endif

@interface VisionCameraFaceDetector (FrameProcessorPluginLoader)
@end

@implementation VisionCameraFaceDetector (FrameProcessorPluginLoader)
+ (void) load {
  [FrameProcessorPluginRegistry addFrameProcessorPlugin:@"detectFaces"
    withInitializer:^FrameProcessorPlugin*(VisionCameraProxyHolder* proxy, NSDictionary* options) {
    return [[VisionCameraFaceDetector alloc] initWithProxy:proxy withOptions:options];
  }];
}
@end
