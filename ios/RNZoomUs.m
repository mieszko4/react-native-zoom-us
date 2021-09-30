#import <ReplayKit/ReplayKit.h>
#import "RNZoomUs.h"

@implementation RNZoomUs
{
  BOOL isInitialized;
  BOOL shouldAutoConnectAudio;
  BOOL hasObservers;
  RCTPromiseResolveBlock initializePromiseResolve;
  RCTPromiseRejectBlock initializePromiseReject;
  RCTPromiseResolveBlock meetingPromiseResolve;
  RCTPromiseRejectBlock meetingPromiseReject;
  // If screenShareExtension is set, the Share Content > Screen option will automatically be
  // enabled in the UI
  NSString *screenShareExtension;

  NSString *jwtToken;
}

- (instancetype)init {
  if (self = [super init]) {
    isInitialized = NO;
    initializePromiseResolve = nil;
    initializePromiseReject = nil;
    shouldAutoConnectAudio = nil;
    meetingPromiseResolve = nil;
    meetingPromiseReject = nil;
    screenShareExtension = nil;
    jwtToken = nil;
  }
  return self;
}

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(isInitialized: (RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
  @try {
    // todo check from ZoomSdk
    resolve(@(isInitialized));
  } @catch (NSError *ex) {
    reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing isInitialized", ex);
  }
}

RCT_EXPORT_METHOD(
  initialize: (NSDictionary *)data
  withSettings: (NSDictionary *)settings
  withResolve: (RCTPromiseResolveBlock)resolve
  withReject: (RCTPromiseRejectBlock)reject
)
{
  if (isInitialized) {
    resolve(@"Already initialize Zoom SDK successfully.");
    return;
  }

  isInitialized = true;

  @try {
    initializePromiseResolve = resolve;
    initializePromiseReject = reject;

    screenShareExtension = data[@"iosScreenShareExtensionId"];
    jwtToken = data[@"jwtToken"];

    MobileRTCSDKInitContext *context = [[MobileRTCSDKInitContext alloc] init];
    context.domain = data[@"domain"];
    context.enableLog = YES;
    context.locale = MobileRTC_ZoomLocale_Default;

    //Note: This step is optional, Method is used for iOS Replaykit Screen share integration,if not,just ignore this step.
    context.appGroupId = data[@"iosAppGroupId"];
    BOOL initializeSuc = [[MobileRTC sharedRTC] initialize:context];
    MobileRTCMeetingSettings *zoomSettings = [[MobileRTC sharedRTC] getMeetingSettings];
    [zoomSettings disableShowVideoPreviewWhenJoinMeeting:settings[@"disableShowVideoPreviewWhenJoinMeeting"]];
    zoomSettings.enableCustomMeeting = settings[@"enableCustomizedMeetingUI"];

    [[MobileRTC sharedRTC] setLanguage:settings[@"language"]];

    MobileRTCAuthService *authService = [[MobileRTC sharedRTC] getAuthService];
    if (authService)
    {
      authService.delegate = self;
      if (jwtToken != nil) {
        authService.jwtToken = data[@"jwtToken"];
      } else {
        authService.clientKey = data[@"clientKey"];
        authService.clientSecret = data[@"clientSecret"];
      }

      [authService sdkAuth];
    } else {
      NSLog(@"onZoomSDKInitializeResult, no authService");
    }
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing initialize", ex);
  }
}

RCT_EXPORT_METHOD(
  startMeeting: (NSDictionary *)data
  withResolve: (RCTPromiseResolveBlock)resolve
  withReject: (RCTPromiseRejectBlock)reject
)
{
  @try {
    meetingPromiseResolve = resolve;
    meetingPromiseReject = reject;

    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (ms) {
      ms.delegate = self;

      MobileRTCMeetingStartParam4WithoutLoginUser * params = [[MobileRTCMeetingStartParam4WithoutLoginUser alloc]init];
      params.userName = data[@"userName"];
      params.meetingNumber = data[@"meetingNumber"];
      params.userID = data[@"userId"];
      params.userType = data[@"userType"];
      params.zak = data[@"zoomAccessToken"];

      MobileRTCMeetError startMeetingResult = [ms startMeetingWithStartParam:params];
      NSLog(@"startMeeting, startMeetingResult=%lu", startMeetingResult);
    }
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing startMeeting", ex);
  }
}

RCT_EXPORT_METHOD(
  joinMeeting: (NSDictionary *)data
  withResolve: (RCTPromiseResolveBlock)resolve
  withReject: (RCTPromiseRejectBlock)reject
)
{
  @try {
    shouldAutoConnectAudio = data[@"autoConnectAudio"];
    meetingPromiseResolve = resolve;
    meetingPromiseReject = reject;

    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (ms) {
      ms.delegate = self;

      MobileRTCMeetingJoinParam * joinParam = [[MobileRTCMeetingJoinParam alloc]init];
      joinParam.userName = data[@"userName"];
      joinParam.meetingNumber = data[@"meetingNumber"];
      joinParam.password =  data[@"password"];
//       joinParam.participantID = data[@"participantID"]; // todo any new keyword?
      joinParam.zak = data[@"zoomAccessToken"];
      joinParam.webinarToken =  data[@"webinarToken"];
      joinParam.noAudio = data[@"noAudio"];
      joinParam.noVideo = data[@"noVideo"];

      MobileRTCMeetError joinMeetingResult = [ms joinMeetingWithJoinParam:joinParam];

      NSLog(@"joinMeeting, joinMeetingResult=%lu", joinMeetingResult);
    }
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing joinMeeting", ex);
  }
}

// todo should be deleted
RCT_EXPORT_METHOD(
  joinMeetingWithPassword: (NSString *)displayName
  withMeetingNo: (NSString *)meetingNo
  withPassword: (NSString *)password
  withResolve: (RCTPromiseResolveBlock)resolve
  withReject: (RCTPromiseRejectBlock)reject
)
{
  @try {
    meetingPromiseResolve = resolve;
    meetingPromiseReject = reject;

    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (ms) {
      ms.delegate = self;

      MobileRTCMeetingJoinParam * joinParam = [[MobileRTCMeetingJoinParam alloc]init];
      joinParam.userName = displayName;
      joinParam.meetingNumber = meetingNo;
      joinParam.password = password;

      MobileRTCMeetError joinMeetingResult = [ms joinMeetingWithJoinParam:joinParam];
      NSLog(@"joinMeeting, joinMeetingResult=%lu", joinMeetingResult);
    }
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing joinMeeting", ex);
  }
}

RCT_EXPORT_METHOD(leaveMeeting: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) return;
    [ms leaveMeetingWithCmd:LeaveMeetingCmd_Leave];
  } @catch (NSError *ex) {
    reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing leaveMeeting", ex);
  }
}

RCT_EXPORT_METHOD(connectAudio: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    [self connectAudio];
    resolve(nil);
  } @catch (NSError *ex) {
    reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing connectAudio", ex);
  }
}

- (void)connectAudio {
  MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
  if (!ms) return;
  [ms connectMyAudio: YES];
  [ms muteMyAudio: NO];
  NSLog(@"connectAudio");
}

RCT_EXPORT_METHOD(isMeetingConnected: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    MobileRTCMeetingState state = [ms getMeetingState];
    resolve(@(state == MobileRTCMeetingState_InMeeting));
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing isMeetingConnected", ex);
  }
}

RCT_EXPORT_METHOD(isMeetingHost: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    resolve(@([ms isMeetingHost]));
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing isMeetingHost", ex);
  }
}

RCT_EXPORT_METHOD(getInMeetingUserIdList: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    NSMutableArray *rnUserList = [[NSMutableArray alloc] init];
    if (ms) {
      NSArray<NSNumber *> *userList = [ms getInMeetingUserList];
      if (userList != nil) {
        [userList enumerateObjectsUsingBlock:^(NSNumber *userId, NSUInteger idx, BOOL *stop) {
            [rnUserList addObject:[userId stringValue]];
        }];
      }
    }
    resolve(rnUserList);
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing getInMeetingUserIdList", ex);
  }
}

RCT_EXPORT_METHOD(muteMyAudio: (BOOL)muted resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    MobileRTCAudioError error = [ms muteMyAudio: muted];
    if (error == 0) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", [NSString stringWithFormat:@"Mute my video error, status: %lu", error], nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing muteMyAudio", ex);
  }
}

RCT_EXPORT_METHOD(muteMyVideo: (BOOL)muted resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    MobileRTCVideoError error = [ms muteMyVideo:muted];
    if (error == 0) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", [NSString stringWithFormat:@"Mute my video error, status: %lu", error], nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing muteMyVideo", ex);
  }
}

RCT_EXPORT_METHOD(muteAttendee: (NSString *)userId muted:(BOOL)muted resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    NSNumber *zoomUserId = @([userId intValue]);
    if ([ms muteUserAudio:muted withUID:zoomUserId]) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Fail to mute attendee", nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing muteAttendee", ex);
  }
}

RCT_EXPORT_METHOD(muteAllAttendee: (BOOL)allowUnmuteSelf resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    if ([ms muteAllUserAudio: allowUnmuteSelf]) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Fail to mute all attendee", nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing muteAllAttendee", ex);
  }
}

RCT_EXPORT_METHOD(startShareScreen: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    if ([ms startAppShare]) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Fail to share screen", nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing startShareScreen", ex);
  }
}

RCT_EXPORT_METHOD(stopShareScreen: (BOOL)muted resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (ms) {
      [ms stopAppShare];
    }
    resolve(nil);
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing stopShareScreen", ex);
  }
}

RCT_EXPORT_METHOD(switchCamera: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    MobileRTCCameraError error = [ms switchMyCamera];
    if (error == 0) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", [NSString stringWithFormat:@"Switch camera error, status: %lu", error], nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing switchCamera", ex);
  }
}

RCT_EXPORT_METHOD(raiseMyHand: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    if ([ms raiseMyHand]) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Fail raise hand", nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing raiseMyHand", ex);
  }
}

RCT_EXPORT_METHOD(lowerMyHand: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
  @try {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (!ms) {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Cannot get meeting service.", nil);
      return;
    }
    if ([ms lowerHand:[ms myselfUserID]]) {
      resolve(nil);
    } else {
      reject(@"ERR_ZOOM_MEETING_CONTROL", @"Fail lower hand", nil);
    }
  } @catch (NSError *ex) {
    reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing lowerMyHand", ex);
  }
}

- (void)onMobileRTCAuthReturn:(MobileRTCAuthError)returnValue {
  NSLog(@"nZoomSDKInitializeResult, errorCode=%d", returnValue);
  [self sendEventWithName:@"AuthEvent" event:[self authErrorName:returnValue]];
  if(returnValue != MobileRTCAuthError_Success) {
    initializePromiseReject(
      @"ERR_ZOOM_INITIALIZATION",
      [NSString stringWithFormat:@"Error: %d", returnValue],
      [NSError errorWithDomain:@"us.zoom.sdk" code:returnValue userInfo:nil]
    );
  } else {
    initializePromiseResolve(@"Initialize Zoom SDK successfully.");
  }
}

- (void)onMeetingReturn:(MobileRTCMeetError)errorCode internalError:(NSInteger)internalErrorCode {
  NSLog(@"onMeetingReturn, error=%d, internalErrorCode=%zd", errorCode, internalErrorCode);
  [self sendEventWithName:@"MeetingEvent" event:[self meetErrorName:errorCode]];

  if (!meetingPromiseResolve) {
    return;
  }

  if (errorCode != MobileRTCMeetError_Success) {
    meetingPromiseReject(
      @"ERR_ZOOM_MEETING",
      [NSString stringWithFormat:@"Error: %d, internalErrorCode=%zd", errorCode, internalErrorCode],
      [NSError errorWithDomain:@"us.zoom.sdk" code:errorCode userInfo:nil]
    );
  } else {
    meetingPromiseResolve(@"Connected to zoom meeting");
  }

  meetingPromiseResolve = nil;
  meetingPromiseReject = nil;
}

- (NSString*)formatStateToString:(MobileRTCMeetingState)state {
    NSString *result = nil;

    // naming synced with android enum MeetingStatus
    switch(state) {
        case MobileRTCMeetingState_Connecting:
            result = @"MEETING_STATUS_CONNECTING";
            break;
        case MobileRTCMeetingState_Idle:
            result = @"MEETING_STATUS_IDLE";
            break;
        case MobileRTCMeetingState_Failed:
            result = @"MEETING_STATUS_FAILED";
            break;
        case MobileRTCMeetingState_WebinarPromote:
            result = @"MEETING_STATUS_WEBINAR_PROMOTE";
            break;
        case MobileRTCMeetingState_WebinarDePromote:
            result = @"MEETING_STATUS_WEBINAR_DEPROMOTE";
            break;
        case MobileRTCMeetingState_InWaitingRoom:
            result = @"MEETING_STATUS_IN_WAITING_ROOM";
            break;
        case MobileRTCMeetingState_WaitingForHost:
            result = @"MEETING_STATUS_WAITINGFORHOST";
            break;
        case MobileRTCMeetingState_Disconnecting:
            result = @"MEETING_STATUS_DISCONNECTING";
            break;
        case MobileRTCMeetingState_InMeeting:
            result = @"MEETING_STATUS_INMEETING";
            break;
        case MobileRTCMeetingState_Reconnecting:
            result = @"MEETING_STATUS_RECONNECTING";
            break;
        case MobileRTCMeetingState_Unknow:
            result = @"MEETING_STATUS_UNKNOWN";
            break;

        // only iOS (guessed naming)
        case MobileRTCMeetingState_WaitingExternalSessionKey:
            result = @"MEETING_STATUS_WAITING_EXTERNAL_SESSION_KEY";
            break;
        case MobileRTCMeetingState_Ended:
            result = @"MEETING_STATUS_ENDED";
            break;
        case MobileRTCMeetingState_Locked:
            result = @"MEETING_STATUS_LOCKED";
            break;
        case MobileRTCMeetingState_Unlocked:
            result = @"MEETING_STATUS_UNLOCKED";
            break;
        case MobileRTCMeetingState_JoinBO:
            result = @"MEETING_STATUS_JOIN_BO";
            break;
        case MobileRTCMeetingState_LeaveBO:
            result = @"MEETING_STATUS_LEAVE_BO";
            break;

        default:
            [NSException raise:NSGenericException format:@"Unexpected state."];
    }

    return result;
}

- (void)onMeetingStateChange:(MobileRTCMeetingState)state {
  NSLog(@"onMeetingStatusChanged, meetingState=%d", state);

  NSString* statusString = [self formatStateToString:state];
  [self sendEventWithName:@"MeetingEvent" event:@"success" status:statusString];

  if (state == MobileRTCMeetingState_InMeeting && shouldAutoConnectAudio == YES) {
    [self connectAudio];
  }

  if (state == MobileRTCMeetingState_InMeeting || state == MobileRTCMeetingState_Idle) {
    if (!meetingPromiseResolve) {
      return;
    }

    meetingPromiseResolve(@"Connected to zoom meeting");

    meetingPromiseResolve = nil;
    meetingPromiseReject = nil;
  }
}

- (void)onMeetingError:(MobileRTCMeetError)errorCode message:(NSString *)message {
  NSLog(@"onMeetingError, errorCode=%d, message=%@", errorCode, message);
  [self sendEventWithName:@"MeetingEvent" event:[self meetErrorName:errorCode]];

  if (!meetingPromiseResolve) {
    return;
  }

  if (errorCode != MobileRTCMeetError_Success) {
    meetingPromiseReject(
      @"ERR_ZOOM_MEETING",
      [NSString stringWithFormat:@"Error: %d, internalErrorCode=%@", errorCode, message],
      [NSError errorWithDomain:@"us.zoom.sdk" code:errorCode userInfo:nil]
    );
  } else {
    meetingPromiseResolve(@"Connected to zoom meeting");
  }

  shouldAutoConnectAudio = nil;
  meetingPromiseResolve = nil;
  meetingPromiseReject = nil;
}

- (void)onMeetingEndedReason:(MobileRTCMeetingEndReason)reason {
  [self sendEventWithName:@"MeetingEvent" event:[self meetingEndReasonName:reason]];
}

#pragma mark - Screen share functionality

- (void)onSinkMeetingActiveShare:(NSUInteger)userID {
  MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
  if (ms) {
    if (userID == 0) {
      [self sendEventWithName:@"MeetingEvent" event:@"screenShareStopped"];
    } else if ([ms isMyself:userID]){
      [self sendEventWithName:@"MeetingEvent" event:@"screenShareStarted"];
    }
  }
}

- (void)onClickShareScreen:(UIViewController *)parentVC {
  if (@available(iOS 12.0, *)) {
    CGRect frame = parentVC.view.bounds;
    RPSystemBroadcastPickerView *pickerView = [[RPSystemBroadcastPickerView alloc] initWithFrame:frame];
    pickerView.preferredExtension = screenShareExtension;
    SEL buttonPressed = NSSelectorFromString(@"buttonPressed:");
    if ([pickerView respondsToSelector:buttonPressed]) {
      [pickerView performSelector:buttonPressed withObject:nil];
    }
  }
}

#pragma mark - https://marketplacefront.zoom.us/sdk/meeting/ios/_mobile_r_t_c_meeting_delegate_8h_source.html


#pragma mark - MobileRTCVideoServiceDelegate

- (void)onSinkMeetingVideoStatusChange:(NSUInteger)userID videoStatus:(MobileRTC_VideoStatus)videoStatus {
  MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];

  if ([ms isMyself:userID]) {
    MobileRTCMeetingUserInfo *userInfo = [ms userInfoByID:[ms myselfUserID]];

    [self sendEventWithName:@"MeetingEvent" params:@{
      @"event": @"myVideoStatusChanged",
      @"userRole": [self getUserRole:[userInfo userRole]],
      @"audioType": @([[userInfo audioStatus] audioType]),
      @"isTalking": @([[userInfo audioStatus] isTalking]),
      @"isMutedAudio": @((BOOL)([[userInfo audioStatus] audioType] == 2 ? YES : [[userInfo audioStatus] isMuted])),
      @"isMutedVideo": @((BOOL)![[userInfo videoStatus] isSending]),
    }];
  }
}

- (void)onSinkMeetingActiveVideo:(NSUInteger)userID {}

- (void)onSinkMeetingVideoStatusChange:(NSUInteger)userID {}

- (void)onMyVideoStateChange {}

- (void)onSpotlightVideoChange:(BOOL)on {}

- (void)onSinkMeetingPreviewStopped {}

- (void)onSinkMeetingActiveVideoForDeck:(NSUInteger)userID {}

- (void)onSinkMeetingVideoQualityChanged:(MobileRTCNetworkQuality)qality userID:(NSUInteger)userID {}

- (void)onSinkMeetingVideoRequestUnmuteByHost:(void (^_Nonnull)(BOOL Accept))completion {}

- (void)onSinkMeetingShowMinimizeMeetingOrBackZoomUI:(MobileRTCMinimizeMeetingState)state {}


#pragma mark - MobileRTCAudioServiceDelegate

- (void)onSinkMeetingMyAudioTypeChange {
  MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
  MobileRTCMeetingUserInfo *userInfo = [ms userInfoByID:[ms myselfUserID]];

  [self sendEventWithName:@"MeetingEvent" params:@{
    @"event": @"myAudioTypeChanged",
    @"userRole": [self getUserRole:[userInfo userRole]],
    @"audioType": @([[userInfo audioStatus] audioType]),
    @"isTalking": @([[userInfo audioStatus] isTalking]),
    @"isMutedAudio": @((BOOL)([[userInfo audioStatus] audioType] == 2 ? YES : [[userInfo audioStatus] isMuted])),
    @"isMutedVideo": @((BOOL)![[userInfo videoStatus] isSending]),
  }];
}

- (void)onSinkMeetingAudioStatusChange:(NSUInteger)userID audioStatus:(MobileRTC_AudioStatus)audioStatus {
  MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];

  if ([ms isMyself:userID]) {
    MobileRTCMeetingUserInfo *userInfo = [ms userInfoByID:[ms myselfUserID]];

    [self sendEventWithName:@"MeetingEvent" params:@{
      @"event": @"myAudioStatusChanged",
      @"userRole": [self getUserRole:[userInfo userRole]],
      @"audioType": @([[userInfo audioStatus] audioType]),
      @"isTalking": @([[userInfo audioStatus] isTalking]),
      @"isMutedAudio": @((BOOL)([[userInfo audioStatus] audioType] == 2 ? YES : [[userInfo audioStatus] isMuted])),
      @"isMutedVideo": @((BOOL)![[userInfo videoStatus] isSending]),
    }];
  }
}

- (void)onSinkMeetingAudioRequestUnmuteByHost {
  [self sendEventWithName:@"MeetingEvent" event:@"askUnMuteAudio"];
}

- (void)onSinkMeetingAudioStatusChange:(NSUInteger)userID {}

- (void)onSinkMeetingAudioTypeChange:(NSUInteger)userID {}

- (void)onAudioOutputChange {}

- (void)onMyAudioStateChange {}


#pragma mark - MobileRTCUserServiceDelegate

- (void)onMyHandStateChange {}

- (void)onInMeetingUserUpdated {}

- (void)onSinkMeetingUserRaiseHand:(NSUInteger)userID {}

- (void)onSinkMeetingUserLowerHand:(NSUInteger)userID {}

- (void)onSinkUserNameChanged:(NSUInteger)userID userName:(NSString *_Nonnull)userName {}

- (void)onClaimHostResult:(MobileRTCClaimHostError)error {}

- (void)onMeetingHostChange:(NSUInteger)userId {
    [self sendEventWithName:@"MeetingEvent" params:@{
    @"event": @"hostChanged",
    @"userId": @(userId)
  }];
}

- (void)onMeetingCoHostChange:(NSUInteger)userId {
    [self sendEventWithName:@"MeetingEvent" params:@{
    @"event": @"coHostChanged",
    @"userId": @(userId)
  }];
}

- (void)onSinkMeetingUserLeft:(NSUInteger)userId {
  [self sendEventWithName:@"MeetingEvent" params:@{
    @"event": @"userLeave",
    @"userList": @[@(userId)]
  }];
}

- (void)onSinkMeetingUserJoin:(NSUInteger)userId {
  [self sendEventWithName:@"MeetingEvent" params:@{
    @"event": @"userJoin",
    @"userList": @[@(userId)]
  }];
}

- (BOOL)respondsToSelector:(SEL)aSelector {
  if (aSelector == @selector(onClickShareScreen:)) {
    return screenShareExtension != nil;
  }
  return [super respondsToSelector:aSelector];
}

#pragma mark - helpers
- (NSString*)getUserRole:(NSInteger)roleCode {
  // TODO: missing USERROLE_PANELIST, USERROLE_BREAKOUTROOM_MODERATOR
  switch (roleCode) {
    case 1: return @"USERROLE_HOST";
    case 2: return @"USERROLE_COHOST";
    case 3: return @"USERROLE_ATTENDEE";
    default: return @"USERROLE_NONE";
  }
}

#pragma mark - React Native event emitters and event handling

- (void)startObserving {
  hasObservers = YES;
}

- (void)stopObserving {
  hasObservers = NO;
}

- (NSArray<NSString *> *)supportedEvents {
  return @[@"AuthEvent", @"MeetingEvent"];
}

- (void)sendEventWithName:(NSString *)name event:(NSString *)event {
  if (hasObservers) {
    [self sendEventWithName:name body:@{@"event": event}];
  }
}
- (void)sendEventWithName:(NSString *)name event:(NSString *)event status:(NSString *)status {
  if (hasObservers) {
    [self sendEventWithName:name body:@{@"event": event, @"status": status}];
  }
}
- (void)sendEventWithName:(NSString *)name params:(NSDictionary *)params {
  if (hasObservers) {
    [self sendEventWithName:name body:params];
  }
}

- (NSString *)authErrorName:(MobileRTCAuthError)error {
  switch (error) {
    case MobileRTCAuthError_ClientIncompatible: return @"clientIncompatible";
    case MobileRTCAuthError_Success: return @"success";
    case MobileRTCAuthError_AccountNotEnableSDK: return @"accountNotEnableSDK"; // iOS only
    case MobileRTCAuthError_AccountNotSupport: return @"accountNotSupport"; // iOS only
    case MobileRTCAuthError_KeyOrSecretEmpty: return @"keyOrSecretEmpty"; // iOS only
    case MobileRTCAuthError_KeyOrSecretWrong: return @"keyOrSecretWrong"; // iOS only
    case MobileRTCAuthError_NetworkIssue: return @"networkIssue"; // iOS only
    case MobileRTCAuthError_None: return @"none"; // iOS only
    case MobileRTCAuthError_OverTime: return @"overTime"; // iOS only
    case MobileRTCAuthError_ServiceBusy: return @"serviceBusy"; // iOS only
    default: return @"unknown";
  }
}

- (NSString *)meetErrorName:(MobileRTCMeetError)error {
  switch (error) {
    case MobileRTCMeetError_InvalidArguments: return @"invalidArguments";
    case MobileRTCMeetError_MeetingClientIncompatible: return @"meetingClientIncompatible";
    case MobileRTCMeetError_MeetingLocked: return @"meetingLocked";
    case MobileRTCMeetError_MeetingNotExist: return @"meetingNotExist";
    case MobileRTCMeetError_MeetingOver: return @"meetingOver";
    case MobileRTCMeetError_MeetingRestricted: return @"meetingRestricted";
    case MobileRTCMeetError_MeetingRestrictedJBH: return @"meetingRestrictedJBH";
    case MobileRTCMeetError_MeetingUserFull: return @"meetingUserFull";
    case MobileRTCMeetError_MMRError: return @"mmrError";
    case MobileRTCMeetError_NetworkError: return @"networkError";
    case MobileRTCMeetError_NoMMR: return @"noMMR";
    case MobileRTCMeetError_RegisterWebinarDeniedEmail: return @"registerWebinarDeniedEmail";
    case MobileRTCMeetError_RegisterWebinarEnforceLogin: return @"registerWebinarEnforceLogin";
    case MobileRTCMeetError_RegisterWebinarFull: return @"registerWebinarFull";
    case MobileRTCMeetError_RegisterWebinarHostRegister: return @"registerWebinarHostRegister";
    case MobileRTCMeetError_RegisterWebinarPanelistRegister: return @"registerWebinarPanelistRegister";
    case MobileRTCMeetError_RemovedByHost: return @"removedByHost";
    case MobileRTCMeetError_SessionError: return @"sessionError";
    case MobileRTCMeetError_Success: return @"success";
    case MobileRTCMeetError_AudioAutoStartError: return @"audioAutoStartError"; // iOS only
    case MobileRTCMeetError_CannotEmitWebRequest: return @"cannotEmitWebRequest"; // iOS only
    case MobileRTCMeetError_CannotStartTokenExpire: return @"cannotStartTokenExpire"; // iOS only
    case MobileRTCMeetError_InAnotherMeeting: return @"inAnotherMeeting"; // iOS only
    case MobileRTCMeetError_InvalidUserType: return @"invalidUserType"; // iOS only
    case MobileRTCMeetError_JoinWebinarWithSameEmail: return @"joinWebinarWithSameEmail"; // iOS only
    case MobileRTCMeetError_MeetingNotStart: return @"meetingNotStart"; // iOS only
    case MobileRTCMeetError_PasswordError: return @"passwordError"; // iOS only
    case MobileRTCMeetError_ReconnectError: return @"reconnectError"; // iOS only
    case MobileRTCMeetError_VanityNotExist: return @"vanityNotExist"; // iOS only
    case MobileRTCMeetError_VBMaximumNum: return @"vbMaximumNum"; // iOS only
    case MobileRTCMeetError_VBNoSupport: return @"vbNoSupport"; // iOS only
    case MobileRTCMeetError_VBRemoveNone: return @"vbRemoveNone"; // iOS only
    case MobileRTCMeetError_VBSaveImage: return @"vbSaveImage"; // iOS only
    // _VBSetError has the same value as _VBBase so we are excluding _VBBase
    case MobileRTCMeetError_VBSetError: return @"vbSetError"; // iOS only
    case MobileRTCMeetError_VideoError: return @"videoError"; // iOS only
    case MobileRTCMeetError_WriteConfigFile: return @"writeConfigFile"; // iOS only
    case MobileRTCMeetError_ZCCertificateChanged: return @"zcCertificateChanged"; // iOS only
    default: return @"unknown";
  }
}

- (NSString *)meetingEndReasonName:(MobileRTCMeetingEndReason)reason {
  switch (reason) {
    case MobileRTCMeetingEndReason_EndByHost: return @"endedByHost";
    case MobileRTCMeetingEndReason_HostEndForAnotherMeeting: return @"endedByHostForAnotherMeeting";
    case MobileRTCMeetingEndReason_SelfLeave: return @"endedBySelf";
    case MobileRTCMeetingEndReason_ConnectBroken: return @"endedConnectBroken";
    case MobileRTCMeetingEndReason_FreeMeetingTimeout: return @"endedFreeMeetingTimeout";
    case MobileRTCMeetingEndReason_JBHTimeout: return @"endedJBHTimeout";
    case MobileRTCMeetingEndReason_RemovedByHost: return @"endedRemovedByHost";
    default: return @"endedUnknownReason";
  }
}

@end
