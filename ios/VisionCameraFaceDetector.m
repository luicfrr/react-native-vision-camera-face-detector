#import <Foundation/Foundation.h>
#import "VisionCameraFaceDetector.h"

#if defined __has_include && __has_include("VisionCameraFaceDetector-Swift.h")
#import "VisionCameraFaceDetector-Swift.h"
#else
#import <VisionCameraCodeScanner/VisionCameraFaceDetector-Swift.h>
#endif

@implementation RegisterPlugins

    + (void) load {
        [FrameProcessorPluginRegistry addFrameProcessorPlugin:@"detectFaces"
                                              withInitializer:^FrameProcessorPlugin*(NSDictionary* options) {
            return [[VisionCameraFaceDetector alloc] init];
        }];
    }

@end
