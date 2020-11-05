
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import <MobileRTC/MobileRTC.h>

#if __has_include(<React/RCTEventEmitter.h>)
#import <React/RCTEventEmitter.h>
#elif __has_include("React/RCTEventEmitter.h")
#import "React/RCTEventEmitter.h"
#else
#import "RCTEventEmitter.h"
#endif

@interface RNZoomUs : RCTEventEmitter <RCTBridgeModule, MobileRTCAuthDelegate, MobileRTCMeetingServiceDelegate, MobileRTCVideoServiceDelegate, MobileRTCUserServiceDelegate >

@end

