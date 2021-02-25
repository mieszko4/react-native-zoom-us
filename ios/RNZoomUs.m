#import <ReplayKit/ReplayKit.h>
#import "RNZoomUs.h"

@implementation RNZoomUs
{
  BOOL isInitialized;
  BOOL hasObservers;
  RCTPromiseResolveBlock initializePromiseResolve;
  RCTPromiseRejectBlock initializePromiseReject;
  RCTPromiseResolveBlock meetingPromiseResolve;
  RCTPromiseRejectBlock meetingPromiseReject;
  // If screenShareExtension is set, the Share Content > Screen option will automatically be
  // enabled in the UI
  NSString *screenShareExtension;
}

- (instancetype)init {
  if (self = [super init]) {
    isInitialized = NO;
    initializePromiseResolve = nil;
    initializePromiseReject = nil;
    meetingPromiseResolve = nil;
    meetingPromiseReject = nil;
    screenShareExtension = nil;
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

    MobileRTCSDKInitContext *context = [[MobileRTCSDKInitContext alloc] init];
    context.domain = data[@"domain"];
    context.enableLog = YES;
    context.locale = MobileRTC_ZoomLocale_Default;

    //Note: This step is optional, Method is uesd for iOS Replaykit Screen share integration,if not,just ignore this step.
    context.appGroupId = data[@"iosAppGroupId"];
    BOOL initializeSuc = [[MobileRTC sharedRTC] initialize:context];
    [[[MobileRTC sharedRTC] getMeetingSettings]
      disableShowVideoPreviewWhenJoinMeeting:settings[@"disableShowVideoPreviewWhenJoinMeeting"]];

    MobileRTCAuthService *authService = [[MobileRTC sharedRTC] getAuthService];
    if (authService)
    {
      authService.delegate = self;

      authService.clientKey = data[@"clientKey"];
      authService.clientSecret = data[@"clientSecret"];

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
      NSLog(@"startMeeting, startMeetingResult=%d", startMeetingResult);
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
    meetingPromiseResolve = resolve;
    meetingPromiseReject = reject;

    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (ms) {
      ms.delegate = self;

      MobileRTCMeetingJoinParam * joinParam = [[MobileRTCMeetingJoinParam alloc]init];
      joinParam.userName = data[@"userName"];
      joinParam.meetingNumber = data[@"meetingNumber"];
      joinParam.password =  data[@"password"];
      joinParam.participantID = data[@"participantID"];
      joinParam.zak = data[@"zoomAccessToken"];
      joinParam.webinarToken =  data[@"webinarToken"];
      joinParam.noAudio = data[@"noAudio"];
      joinParam.noVideo = data[@"noVideo"];

      MobileRTCMeetError joinMeetingResult = [ms joinMeetingWithJoinParam:joinParam];

      NSLog(@"joinMeeting, joinMeetingResult=%d", joinMeetingResult);
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
      NSLog(@"joinMeeting, joinMeetingResult=%d", joinMeetingResult);
    }
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing joinMeeting", ex);
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

- (void)onMeetingStateChange:(MobileRTCMeetingState)state {
  NSLog(@"onMeetingStatusChanged, meetingState=%d", state);

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

  meetingPromiseReject(
    @"ERR_ZOOM_MEETING",
    [NSString stringWithFormat:@"Error: %d, internalErrorCode=%@", errorCode, message],
    [NSError errorWithDomain:@"us.zoom.sdk" code:errorCode userInfo:nil]
  );

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
    } else if (userID == [ms myselfUserID]){
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

- (BOOL)respondsToSelector:(SEL)aSelector
{
    if (aSelector == @selector(onClickShareScreen:)) {
        return screenShareExtension != nil;
    }
    return [super respondsToSelector:aSelector];
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

- (NSString *)authErrorName:(MobileRTCAuthError)error {
  switch (error) {
    case MobileRTCAuthError_Success: return @"success";
    case MobileRTCAuthError_KeyOrSecretEmpty: return @"keyOrSecretEmpty"; // iOS only
    case MobileRTCAuthError_KeyOrSecretWrong: return @"keyOrSecretWrong"; // iOS only
    case MobileRTCAuthError_AccountNotSupport: return @"accountNotSupport"; // iOS only
    case MobileRTCAuthError_AccountNotEnableSDK: return @"accountNotEnableSDK"; // iOS only
    case MobileRTCAuthError_ServiceBusy: return @"serviceBusy"; // iOS only
    case MobileRTCAuthError_None: return @"none"; // iOS only
    case MobileRTCAuthError_OverTime: return @"overTime"; // iOS only
    case MobileRTCAuthError_NetworkIssue: return @"networkIssue"; // iOS only
    case MobileRTCAuthError_ClientIncompatible: return @"clientIncompatible";
    default: return @"unknown";
  }
}

- (NSString *)meetErrorName:(MobileRTCMeetError)error {
  switch (error) {
    case MobileRTCMeetError_Success: return @"success";
    case MobileRTCMeetError_NetworkError: return @"networkError";
    case MobileRTCMeetError_ReconnectError: return @"reconnectError"; // iOS only
    case MobileRTCMeetError_MMRError: return @"mmrError";
    case MobileRTCMeetError_PasswordError: return @"passwordError"; // iOS only
    case MobileRTCMeetError_SessionError: return @"sessionError";
    case MobileRTCMeetError_MeetingOver: return @"meetingOver";
    case MobileRTCMeetError_MeetingNotStart: return @"meetingNotStart"; // iOS only
    case MobileRTCMeetError_MeetingNotExist: return @"meetingNotExist";
    case MobileRTCMeetError_MeetingUserFull: return @"meetingUserFull";
    case MobileRTCMeetError_MeetingClientIncompatible: return @"meetingClientIncompatible";
    case MobileRTCMeetError_NoMMR: return @"noMMR";
    case MobileRTCMeetError_MeetingLocked: return @"meetingLocked";
    case MobileRTCMeetError_MeetingRestricted: return @"meetingRestricted";
    case MobileRTCMeetError_MeetingRestrictedJBH: return @"meetingRestrictedJBH";
    case MobileRTCMeetError_CannotEmitWebRequest: return @"cannotEmitWebRequest"; // iOS only
    case MobileRTCMeetError_CannotStartTokenExpire: return @"cannotStartTokenExpire"; // iOS only
    case MobileRTCMeetError_VideoError: return @"videoError"; // iOS only
    case MobileRTCMeetError_AudioAutoStartError: return @"audioAutoStartError"; // iOS only
    case MobileRTCMeetError_RegisterWebinarFull: return @"registerWebinarFull";
    case MobileRTCMeetError_RegisterWebinarHostRegister: return @"registerWebinarHostRegister";
    case MobileRTCMeetError_RegisterWebinarPanelistRegister: return @"registerWebinarPanelistRegister";
    case MobileRTCMeetError_RegisterWebinarDeniedEmail: return @"registerWebinarDeniedEmail";
    case MobileRTCMeetError_RegisterWebinarEnforceLogin: return @"registerWebinarEnforceLogin";
    case MobileRTCMeetError_ZCCertificateChanged: return @"zcCertificateChanged"; // iOS only
    case MobileRTCMeetError_VanityNotExist: return @"vanityNotExist"; // iOS only
    case MobileRTCMeetError_JoinWebinarWithSameEmail: return @"joinWebinarWithSameEmail"; // iOS only
    case MobileRTCMeetError_WriteConfigFile: return @"writeConfigFile"; // iOS only
    case MobileRTCMeetError_RemovedByHost: return @"removedByHost";
    case MobileRTCMeetError_InvalidArguments: return @"invalidArguments";
    case MobileRTCMeetError_InvalidUserType: return @"invalidUserType"; // iOS only
    case MobileRTCMeetError_InAnotherMeeting: return @"inAnotherMeeting"; // iOS only
    // _VBSetError has the same value as _VBBase so we are excluding _VBBase
    case MobileRTCMeetError_VBSetError: return @"vbSetError"; // iOS only
    case MobileRTCMeetError_VBMaximumNum: return @"vbMaximumNum"; // iOS only
    case MobileRTCMeetError_VBSaveImage: return @"vbSaveImage"; // iOS only
    case MobileRTCMeetError_VBRemoveNone: return @"vbRemoveNone"; // iOS only
    case MobileRTCMeetError_VBNoSupport: return @"vbNoSupport"; // iOS only
    default: return @"unknown";
  }
}

- (NSString *)meetingEndReasonName:(MobileRTCMeetingEndReason)reason {
  switch (reason) {
    case MobileRTCMeetingEndReason_SelfLeave: return @"endedBySelf";
    case MobileRTCMeetingEndReason_RemovedByHost: return @"endedRemovedByHost";
    case MobileRTCMeetingEndReason_EndByHost: return @"endedByHost";
    case MobileRTCMeetingEndReason_JBHTimeout: return @"endedJBHTimeout";
    case MobileRTCMeetingEndReason_FreeMeetingTimeout: return @"endedFreeMeetingTimeout";
    case MobileRTCMeetingEndReason_HostEndForAnotherMeeting: return @"endedByHostForAnotherMeeting";
    case MobileRTCMeetingEndReason_ConnectBroken: return @"endedConnectBroken";
    default: return @"endedUnknownReason";
  }
}

@end
