#import <ReplayKit/ReplayKit.h>
#import "RNZoomUs.h"

@implementation RNZoomUs
{
  BOOL isInitialized;
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

    screenShareExtension = data[@"screenShareExtension"];

    MobileRTCSDKInitContext *context = [[MobileRTCSDKInitContext alloc] init];
    context.domain = data[@"domain"];
    context.enableLog = YES;
    context.locale = MobileRTC_ZoomLocale_Default;

    //Note: This step is optional, Method is uesd for iOS Replaykit Screen share integration,if not,just ignore this step.
    context.appGroupId = data[@"appGroupId"];
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

@end
