
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import <MobileRTC/MobileRTC.h>

@interface RNZoomUs : NSObject <RCTBridgeModule, MobileRTCAuthDelegate, MobileRTCMeetingServiceDelegate>

@end

