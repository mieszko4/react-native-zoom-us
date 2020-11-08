
#import "RNZoomUs.h"

@implementation RNZoomUs
{
  BOOL isInitialized;
  RCTPromiseResolveBlock initializePromiseResolve;
  RCTPromiseRejectBlock initializePromiseReject;
  RCTPromiseResolveBlock meetingPromiseResolve;
  RCTPromiseRejectBlock meetingPromiseReject;
}

- (instancetype)init {
  if (self = [super init]) {
    isInitialized = NO;
    initializePromiseResolve = nil;
    initializePromiseReject = nil;
    meetingPromiseResolve = nil;
    meetingPromiseReject = nil;
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


    MobileRTCSDKInitContext *context = [[MobileRTCSDKInitContext alloc] init];
    context.domain = data[@"domain"];;
    context.enableLog = YES;
    context.locale = MobileRTC_ZoomLocale_Default;

    //Note: This step is optional, Method is uesd for iOS Replaykit Screen share integration,if not,just ignore this step.
    // context.appGroupId = @"group.zoom.us.MobileRTCSampleExtensionReplayKit";
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
      joinParam.password =  data[@"password"];
      joinParam.participantID = data[@"participantID"];
      joinParam.zak = data[@"zoomAccessToken"];
      joinParam.webinarToken =  data[@"webinarToken"];
      joinParam.noAudio = data[@"noAudio"];
      joinParam.noVideo = data[@"noVideo"];
        
        if (data[@"vanityID"]) {
            joinParam.vanityID = data[@"vanityID"];
        } else {
            joinParam.meetingNumber = data[@"meetingNumber"];
        }

      MobileRTCMeetError joinMeetingResult = [ms joinMeetingWithJoinParam:joinParam];

      NSLog(@"joinMeeting, joinMeetingResult=%d", joinMeetingResult);
    }
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing joinMeeting", ex);
  }
}

RCT_EXPORT_METHOD(
  joinMeetingWithWebUrl: (NSString *)url
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

      MobileRTCMeetError joinMeetingResult = [ms handZoomWebUrl:url];

      NSLog(@"joinMeetingWithWebUrl, joinMeetingResult=%d", joinMeetingResult);
    }
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing joinMeetingWithWebUrl", ex);
  }
}

RCT_REMAP_METHOD(getMyUserMeetingInfo,
                 withResolve:(RCTPromiseResolveBlock)resolve
                 withReject:(RCTPromiseRejectBlock)reject
)
{
  @try {
      MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
      if (!ms) {
          return reject(@"No meeting service", @"could not approach zoom meeting", nil);
      }
      
      MobileRTCMeetingState meetingState = ms.getMeetingState;
      if (meetingState == MobileRTCMeetingState_InMeeting) {
          return resolve([self getUSerInfoByUserId:[ms myselfUserID]]);
      }
      
      reject(@"Not in meeting", @"user has not joined meeting", nil);
      
  } @catch (NSError *ex) {
      reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing getMyUserMeetingInfo", ex);
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

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"InMeetingEvent"];
}

- (void) notifyInMeetingEvent:(NSString *)event params:(NSDictionary *)params {
    NSDictionary *body = @{
        @"event": event,
        @"payload": params
    };
    [self sendEventWithName:@"InMeetingEvent" body:body];
}

- (NSDictionary *)getUSerInfoByUserId:(NSUInteger)userID {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    MobileRTCMeetingUserInfo *userInfo = [ms userInfoByID:userID];
    return @{
        @"userId": [NSString stringWithFormat:@"%li",  userID],
        @"name": userInfo.userName
        // @"participantId": userInfo.participantID
    };
}

- (void)onSinkMeetingActiveVideo:(NSUInteger)userID {
    [self notifyInMeetingEvent:@"meeting.user.video.active" params:[self getUSerInfoByUserId:userID]];
}

- (void)onSinkMeetingVideoStatusChange:(NSUInteger)userID {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    MobileRTCMeetingUserInfo *userInfo = [ms userInfoByID:userID];
    BOOL active = [[userInfo videoStatus] isSending];
    [self notifyInMeetingEvent:@"meeting.user.video.status" params:@{
        @"userId": [NSString stringWithFormat:@"%li",  userID],
        @"name": userInfo.userName,
        @"active": [NSNumber numberWithBool:active]
    }];
}

- (void)onSinkMeetingAudioStatusChange:(NSUInteger)userID {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    MobileRTCMeetingUserInfo *userInfo = [ms userInfoByID:userID];
    BOOL muted = [[userInfo audioStatus] isMuted];
    [self notifyInMeetingEvent:@"meeting.user.audio.status" params:@{
        @"userId": [NSString stringWithFormat:@"%li",  userID],
        @"name": userInfo.userName,
        @"muted": [NSNumber numberWithBool:muted]
    }];
}

- (void)onSinkMeetingActiveVideoForDeck:(NSUInteger)userID {
    [self notifyInMeetingEvent:@"meeting.user.video.speaker" params:[self getUSerInfoByUserId:userID]];
}

- (void)onSinkMeetingUserJoin:(NSUInteger)userID {
    [self notifyInMeetingEvent:@"meeting.user.joined" params:[self getUSerInfoByUserId:userID]];
}

- (void)onSinkMeetingUserLeft:(NSUInteger)userID {
    [self notifyInMeetingEvent:@"meeting.user.left" params:[self getUSerInfoByUserId:userID]];
}

@end
