//
//  RNZoomUsVideoView.h
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/11.
//
//

#if __has_include("React/RCTViewManager.h")
#import "React/RCTViewManager.h"
#else
#import "RCTViewManager.h"
#endif

#import <UIKit/UIKit.h>
#import <MobileRTC/MobileRTCMeetingDelegate.h>
#import "CustomMeetingViewController.h"
#import "RCTEventEmitter.h"
@interface RNZoomUsVideoView : UIView<MobileRTCAuthDelegate, MobileRTCMeetingServiceDelegate, MobileRTCAnnotationServiceDelegate, MobileRTCWaitingRoomServiceDelegate, MobileRTCShareActionDelegate>

@property(nonatomic, strong)CustomMeetingViewController* rnZoomUsVideoViewController;
@property (nonatomic, strong) UILabel *label;
@property(nonatomic, copy)RCTBubblingEventBlock onSinkMeetingUserLeft;
@property(nonatomic, copy)RCTBubblingEventBlock onSinkMeetingUserJoin;
@property(nonatomic, copy)RCTBubblingEventBlock onMeetingStateChange;
@property(nonatomic, copy)RCTBubblingEventBlock onInMeetingUserCount;
@property(nonatomic, copy)RCTBubblingEventBlock onBOStatusChanged;
@property(nonatomic, copy)RCTBubblingEventBlock onHasAttendeeRightsNotification;
@property(nonatomic, copy)RCTBubblingEventBlock onMeetingAudioRequestUnmuteByHost;
@property(nonatomic, copy)RCTBubblingEventBlock onMeetingVideoRequestUnmuteByHost;
@property(nonatomic, copy)RCTBubblingEventBlock onSinkMeetingAudioStatusChange;
@property(nonatomic, copy)RCTBubblingEventBlock onMeetingPreviewStopped;
@property(nonatomic, copy)RCTBubblingEventBlock onSinkMeetingVideoStatusChange;
@property(nonatomic, copy)RCTBubblingEventBlock onChatMessageNotification;
@property(nonatomic, copy)RCTBubblingEventBlock onChatMsgDeleteNotification;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, NSNumber *> *lastVideoStatusByUserID;
@property (nonatomic, assign) BOOL audioIsConnected;
@property (nonatomic, assign) BOOL isInMeeting;



- (void)setMuteMyAudio:(BOOL*)muteMyAudio;
- (void)setMuteMyCamera:(BOOL*)muteMyCamera;
- (void)setFullScreen:(BOOL*)fullScreen;
@end
