
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import "RCTEventEmitter.h"
#import <MobileRTC/MobileRTC.h>
#import "CustomMeetingViewController.h"
#import "ProviderDelegate.h"
#import <AVKit/AVKit.h>

@interface RNZoomUs : RCTEventEmitter<RCTBridgeModule, MobileRTCAuthDelegate, MobileRTCMeetingServiceDelegate, MobileRTCAnnotationServiceDelegate, MobileRTCWaitingRoomServiceDelegate, UIAlertViewDelegate, AVPictureInPictureControllerDelegate>
@end

