#import <ReplayKit/ReplayKit.h>
#import "RNZoomUs.h"
#import <React/RCTViewManager.h>
#import "ProviderDelegate.h"
#import "GlobalData.h"
#import <AVFoundation/AVFoundation.h>
#import "MobileRTC/MobileRTCMeetingService+Audio.h"
@implementation RNZoomUs
{
    BOOL isInitialized;
    BOOL shouldAutoConnectAudio;
    BOOL hasObservers;
    BOOL enableCustomMeeting;
    BOOL disableShowVideoPreviewWhenJoinMeeting;
    BOOL disableMinimizeMeeting;
    BOOL disableClearWebKitCache;
    BOOL meetingShareHidden;
    BOOL meetingVideoHidden;
    BOOL meetingAudioHidden;
    BOOL closeCaptionHidden;
    BOOL disableCloudWhiteboard;
    BOOL meetingMoreHidden;
    
    RCTPromiseResolveBlock initializePromiseResolve;
    RCTPromiseRejectBlock initializePromiseReject;
    RCTPromiseResolveBlock meetingPromiseResolve;
    RCTPromiseRejectBlock meetingPromiseReject;
    // If screenShareExtension is set, the Share Content > Screen option will automatically be
    // enabled in the UI
    NSString *screenShareExtension;
    
    NSString *jwtToken;
    ProviderDelegate *providerDelegate;
    CXCallController *callController;
}

- (instancetype)init {
    if (self = [super init]) {
        isInitialized = NO;
        shouldAutoConnectAudio = NO;
        enableCustomMeeting = NO;
        disableShowVideoPreviewWhenJoinMeeting = YES;
        disableMinimizeMeeting = NO;
        disableClearWebKitCache = NO;
        initializePromiseResolve = nil;
        initializePromiseReject = nil;
        meetingPromiseResolve = nil;
        meetingPromiseReject = nil;
        screenShareExtension = nil;
        jwtToken = nil;
        providerDelegate = nil;
        callController = nil;
    }
    
    if (self) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(audioRouteChanged:)
                                                     name:AVAudioSessionRouteChangeNotification
                                                   object:nil];
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(appDidBecomeActive:)
                                                     name:UIApplicationDidBecomeActiveNotification
                                                   object:nil];
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

# pragma mark - UI Native Component

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
    providerDelegate = [[ProviderDelegate alloc] init];
    callController = [[CXCallController alloc] init];
    
    @try {
        initializePromiseResolve = resolve;
        initializePromiseReject = reject;
        
        screenShareExtension = data[@"iosScreenShareExtensionId"];
        jwtToken = data[@"jwtToken"];
        
        MobileRTCSDKInitContext *context = [[MobileRTCSDKInitContext alloc] init];
        context.domain = data[@"domain"];
        context.enableLog = NO;
        context.locale = MobileRTC_ZoomLocale_Default;
        
        //Note: This step is optional, Method is used for iOS Replaykit Screen share integration,if not,just ignore this step.
        context.appGroupId = data[@"iosAppGroupId"];
        
        if (settings[@"enableCustomizedMeetingUI"]) {
            enableCustomMeeting = [[settings objectForKey:@"enableCustomizedMeetingUI"] boolValue];
        }
        context.enableCustomizeMeetingUI = enableCustomMeeting;
        
        if (settings[@"disableShowVideoPreviewWhenJoinMeeting"]) {
            disableShowVideoPreviewWhenJoinMeeting = [[settings objectForKey:@"disableShowVideoPreviewWhenJoinMeeting"] boolValue];
        }
        
        if (settings[@"disableMinimizeMeeting"]) {
            disableMinimizeMeeting = [[settings objectForKey:@"disableMinimizeMeeting"] boolValue];
        }
        
        if (settings[@"disableClearWebKitCache"]) {
            disableClearWebKitCache = [[settings objectForKey:@"disableClearWebKitCache"] boolValue];
        }
        
        if (settings[@"meetingShareHidden"]) {
            meetingShareHidden = [[settings objectForKey:@"meetingShareHidden"] boolValue];
        }
        
        if (settings[@"meetingVideoHidden"]) {
            meetingVideoHidden = [[settings objectForKey:@"meetingVideoHidden"] boolValue];
        }
        
        if (settings[@"meetingAudioHidden"]) {
            meetingAudioHidden = [[settings objectForKey:@"meetingAudioHidden"] boolValue];
        }
        
        if (settings[@"closeCaptionHidden"]) {
            closeCaptionHidden = [[settings objectForKey:@"closeCaptionHidden"] boolValue];
        }
        
        if (settings[@"disableCloudWhiteboard"]) {
            disableCloudWhiteboard = [[settings objectForKey:@"disableCloudWhiteboard"] boolValue];
        }
        
        if (settings[@"meetingMoreHidden"]) {
            meetingMoreHidden = [[settings objectForKey:@"meetingMoreHidden"] boolValue];
        }
        
        [[MobileRTC sharedRTC] setLanguage:settings[@"language"]];
        
        BOOL initializeSuc = [[MobileRTC sharedRTC] initialize:context];
        MobileRTCAuthService *authService = [[MobileRTC sharedRTC] getAuthService];
        if (authService)
        {
            authService.delegate = self;
            authService.jwtToken = data[@"jwtToken"];
            
            [authService sdkAuth];
        } else {
            NSLog(@"onZoomSDKInitializeResult, no authService");
        }
    } @catch (NSError *ex) {
        reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing initialize", ex);
    }
}


RCT_EXPORT_METHOD(
                  cleanup: (RCTPromiseResolveBlock)resolve
                  withReject: (RCTPromiseRejectBlock)reject
                  )
{
    if (!isInitialized) {
        resolve(@"Already initialize Zoom SDK successfully.");
        return;
    }
    
    [[GlobalData sharedInstance] setGlobalIsInMeeting:NO];
    @try {
        isInitialized = NO;
        shouldAutoConnectAudio = NO;
        enableCustomMeeting = NO;
        disableShowVideoPreviewWhenJoinMeeting = YES;
        disableMinimizeMeeting = NO;
        disableClearWebKitCache = NO;
        initializePromiseResolve = nil;
        initializePromiseReject = nil;
        meetingPromiseResolve = nil;
        meetingPromiseReject = nil;
        screenShareExtension = nil;
        jwtToken = nil;
        providerDelegate = nil;
        callController = nil;
        [[MobileRTC sharedRTC] cleanup];
        resolve(@"Already cleanup Zoom SDK successfully.");
    } @catch (NSError *ex) {
        reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing cleanup", ex);
    }
}

- (void)setMeetingSettings {
    MobileRTCMeetingSettings *zoomSettings = [[MobileRTC sharedRTC] getMeetingSettings];
    if (zoomSettings != nil) {
        [zoomSettings enableVideoCallPictureInPicture:YES];
        //        [zoomSettings setMeetingChatHidden:YES];
        [zoomSettings setAutoConnectInternetAudio:YES];
        [zoomSettings disableDriveMode:YES];
        [zoomSettings disableCopyMeetingUrl:YES];
        
        [zoomSettings disableShowVideoPreviewWhenJoinMeeting:disableShowVideoPreviewWhenJoinMeeting];
        [zoomSettings disableMinimizeMeeting:disableMinimizeMeeting];
        [zoomSettings disableClearWebKitCache:disableClearWebKitCache];
        // custom default UI
        [zoomSettings setMeetingShareHidden:meetingShareHidden];
        [zoomSettings setMeetingVideoHidden:meetingVideoHidden];
        [zoomSettings setMeetingAudioHidden:meetingAudioHidden];
        [zoomSettings setCloseCaptionHidden:closeCaptionHidden];
        [zoomSettings disableCloudWhiteboard:disableCloudWhiteboard];
        [zoomSettings setMeetingMoreHidden:meetingMoreHidden];
        
        [zoomSettings setMeetingInviteUrlHidden:YES];
        [zoomSettings setMeetingPasswordHidden:YES];
        [zoomSettings setMeetingInviteHidden:YES];
        [zoomSettings setMeetingTitleHidden:YES];
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
        [self configureAudioSession];
        shouldAutoConnectAudio = [[data objectForKey:@"autoConnectAudio"] boolValue];
        meetingPromiseResolve = resolve;
        meetingPromiseReject = reject;
        
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
        
        if (ms) {
            ms.delegate = self;
            
            MobileRTCMeetingJoinParam * joinParam = [[MobileRTCMeetingJoinParam alloc]init];
            joinParam.userName = data[@"userName"];
            joinParam.meetingNumber = data[@"meetingNumber"];
            joinParam.password =  data[@"password"];
            //            joinParam.participantID = data[@"participantID"]; // todo any new keyword?
            joinParam.zak = data[@"zoomAccessToken"];
            joinParam.webinarToken =  data[@"webinarToken"];
            joinParam.noAudio = data[@"noAudio"];
            joinParam.noVideo = data[@"noVideo"];
            
            MobileRTCMeetError joinMeetingResult = [ms joinMeetingWithJoinParam:joinParam];
            
            NSLog(@"RNZoomUs onJoinaMeeting ret: %@", joinMeetingResult == MobileRTCMeetError_Success ? @"Success" : @(joinMeetingResult));
            resolve(@(YES));
        }
    } @catch (NSError *ex) {
        reject(@"ERR_UNEXPECTED_EXCEPTION", @"Executing joinMeeting", ex);
    }
}

- (void)configureAudioSession {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    NSError *error = nil;
    
    // Đặt category phù hợp: playAndRecord để có thể vừa thu vừa phát
    BOOL success = [session setCategory:AVAudioSessionCategoryPlayAndRecord
                            withOptions:AVAudioSessionCategoryOptionDefaultToSpeaker
                                  error:&error];
    
    if (!success) {
        NSLog(@"[Audio] Failed to set category: %@", error);
    }
    
    // Kích hoạt session
    success = [session setActive:YES error:&error];
    if (!success) {
        NSLog(@"[Audio] Failed to activate session: %@", error);
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
    MobileRTCMeetingSettings *zoomSettings = [[MobileRTC sharedRTC] getMeetingSettings];
    if (!ms || !isInitialized) return;
    [zoomSettings setAutoConnectInternetAudio:YES];
    [ms resetMeetingAudioSession];
    
    [ms connectMyAudio: YES];
    //    [ms connectMyAudio: YES];
    [ms muteMyAudio: YES];
    [ms muteMyVideo: YES];
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
        MobileRTCSDKError error = [ms muteMyVideo:muted];
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

RCT_EXPORT_METHOD(stopShareScreen: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
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

RCT_EXPORT_METHOD(joinBO: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCBOAttendee *attendee = [[[MobileRTC sharedRTC] getMeetingService] getAttedeeHelper];
        
        if (!attendee) {
            reject(@"BO_UNASSIGNED", @"BO_UNASSIGNED", nil);
            return;
        }
        
        BOOL ret = [attendee joinBO];
        NSLog(@"attendee join BO: %@", ret? @"Success": @"Fail");
        resolve(@(ret));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing joinBO", ex);
    }
}

RCT_EXPORT_METHOD(leaveBO: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCBOAttendee *attendee = [[[MobileRTC sharedRTC] getMeetingService] getAttedeeHelper];
        
        if (!attendee) {
            NSLog(@"no object");
            return;
        }
        
        BOOL ret = [attendee leaveBO];
        NSLog(@"attendee leave BO: %@", ret? @"Success": @"Fail");
        resolve(@(ret));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing leaveBO", ex);
    }
}

RCT_EXPORT_METHOD(requestForHelp: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCBOAttendee *attendee = [[[MobileRTC sharedRTC] getMeetingService] getAttedeeHelper];
        
        if (!attendee) {
            NSLog(@"no object");
            return;
        }
        
        BOOL requestResult = [attendee requestForHelp];
        NSString *resultMsg = requestResult? @"succeed" : @"failed";
        resolve(@(requestResult));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing requestForHelp", ex);
    }
}

RCT_EXPORT_METHOD(isHostInThisBO: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCBOAttendee *attendee = [[[MobileRTC sharedRTC] getMeetingService] getAttedeeHelper];
        
        if (!attendee) {
            NSLog(@"no object");
            return;
        }
        
        BOOL requestResult = [attendee isHostInThisBO];
        NSString *resultMsg = requestResult? @"succeed" : @"failed";
        resolve(@(requestResult));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing requestForHelp", ex);
    }
}

RCT_EXPORT_METHOD(getAllChatMessageID: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
        if (ms) {
            NSMutableArray *dataSource = [[ms getAllChatMessageID] mutableCopy];
            if (dataSource != nil) {
                resolve(dataSource);
            }
        }
        resolve(nil);
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing getAllChatMessageID", ex);
    }
}

RCT_EXPORT_METHOD(sendChatMsg: (NSDictionary *)msgData resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
        if (ms) {
            NSNumber *receiverIdNumber = msgData[@"receiverId"];
            NSInteger receiverId = [receiverIdNumber integerValue];
            NSString *content = msgData[@"content"];
            NSNumber *hostIdNumber = msgData[@"hostId"];
            NSInteger hostId = [hostIdNumber integerValue];
            NSString *messageToHost = msgData[@"messageToHost"];
            NSString *previousChatId = msgData[@"previousChatId"];
            MobileRTCChatMessageType chatMessageType =(MobileRTCChatMessageType)[msgData[@"chatMessageType"] unsignedIntegerValue];
            MobileRTCMeetingChatBuilder *builder = [[MobileRTCMeetingChatBuilder alloc] init];
            MobileRTCMeetingChat *newChat = [[[[builder setContent:content] setReceiver:receiverId] setMessageType:chatMessageType] build];
            bool ret = [ms sendChatMsg:newChat];
            NSLog(@"sendCommentsChatMsg==>%@", @(ret));
            if (messageToHost.length > 0 && hostId != 0) {
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2000 * NSEC_PER_MSEC)), dispatch_get_main_queue(), ^{
                    [ms deleteChatMessage:previousChatId];
                    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2000 * NSEC_PER_MSEC)), dispatch_get_main_queue(), ^{
                        MobileRTCMeetingChat *newReplaceChat = [[[[builder setContent:messageToHost] setReceiver:hostId] setMessageType:MobileRTCChatMessageType_To_Individual] build];
                        [ms sendChatMsg:newReplaceChat];});
                });
            }
        }
        resolve(@(YES));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing sendChatMsg", ex);
    }
}

RCT_EXPORT_METHOD(deleteChatMessage: (NSString * _Nullable)msgId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
        if (ms) {
            bool isCanDelete =[ms isChatMessageCanBeDeleted:msgId];
            bool ret = [ms deleteChatMessage:msgId];
            NSLog(@"deleteChatMessage==>%@", @(ret));
        }
        resolve(@(YES));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing sendChatMsg", ex);
    }
}
RCT_EXPORT_METHOD(isHostUser: (NSUInteger)userId resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService] ;
        
        if (!ms) {
            NSLog(@"no object");
            return;
        }
        
        BOOL requestResult = [ms isHostUser:userId];
        resolve(@(requestResult));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing isHostUser", ex);
    }
}

RCT_EXPORT_METHOD(getMyselfUserID: (RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService] ;
        
        if (!ms) {
            NSLog(@"no object");
            return;
        }
        
        NSUInteger myselfUserID = [ms myselfUserID];
        resolve(@(myselfUserID));
    } @catch (NSError *ex) {
        reject(@"ERR_ZOOM_MEETING_CONTROL", @"Executing isHostUser", ex);
    }
}
/*
 NOTE: this is only required for android
 RCT_EXPORT_METHOD(addListener : (NSString *)eventName) {
 // Keep: Required for RN built in Event Emitter Calls.
 }
 RCT_EXPORT_METHOD(removeListeners : (NSInteger)count) {
 // Keep: Required for RN built in Event Emitter Calls.
 }
 */
- (void)appDidBecomeActive:(NSNotification *)notification {
    [self connectAudio];
}
- (void)audioRouteChanged:(NSNotification *)notification {
    NSDictionary *userInfo = notification.userInfo;
    AVAudioSessionRouteChangeReason reason = [userInfo[AVAudioSessionRouteChangeReasonKey] unsignedIntegerValue];
    
    if (reason == AVAudioSessionRouteChangeReasonNewDeviceAvailable) {
        NSLog(@"RNZoomUs Headphones connected");
    } else if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
        NSLog(@"RNZoomUs Headphones disconnected");
    }
    [self updateAudioOutput];
}
- (void)updateAudioOutput {
    BOOL isInmeeting = [[GlobalData sharedInstance] globalIsInMeeting];
    if (!isInmeeting) return;
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    NSError *error = nil;
    
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    MobileRTCAudioOutput audioOutput = [ms myAudioOutputDescription];
    NSLog(@"RNZoomUs MobileRTCAudioOutput: %lu", (unsigned long)audioOutput);
    [audioSession setActive:YES error:&error];
    if (error) {
        NSLog(@"RNZoomUs Error activating audio session: %@", error.localizedDescription);
        [self connectAudio];
        return;
    }
    AVAudioSessionRouteDescription *currentRoute = audioSession.currentRoute;
    BOOL isHeadphonesConnected = NO;
    for (AVAudioSessionPortDescription *output in currentRoute.outputs) {
        NSLog(@"RNZoomUs PortType: %@", output.portType );
        NSArray *headphoneTypes = @[AVAudioSessionPortHeadphones,
                                    AVAudioSessionPortBluetoothHFP,
                                    AVAudioSessionPortBluetoothLE,
                                    AVAudioSessionPortBluetoothA2DP];
        NSArray *receiverTypes = @[AVAudioSessionPortBuiltInReceiver];
        if ([headphoneTypes containsObject:output.portType]) {
            isHeadphonesConnected = YES;
            break;
        }
    }
    
    if (!isHeadphonesConnected) {
        [audioSession overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:&error];
        if (error) {
            NSLog(@"RNZoomUs Error overriding to speaker: %@", error.localizedDescription);
            [self connectAudio];
        }
    }
}
- (void)audioSessionError:(NSNotification *)notification {
    NSLog(@"[AudioSession] Notification: %@", notification.name);
    NSLog(@"UserInfo: %@", notification.userInfo);
}

- (void)onMeetingParameterNotification:(MobileRTCMeetingParameter *_Nullable)meetingParam {}

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
        [self setMeetingSettings];
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
    
    // naming taken from ios enum (https://marketplacefront.zoom.us/sdk/meeting/ios/_mobile_r_t_c_constants_8h.html#a04b17e9f78d7ddc089b7806c502bee4f)
    // and synced with android enum MeetingStatus (https://zoom.github.io/zoom-sdk-android/us/zoom/sdk/MeetingStatus.html)
    switch(state) {
        case MobileRTCMeetingState_Idle:
            result = @"MEETING_STATUS_IDLE";
            break;
        case MobileRTCMeetingState_Connecting:
            result = @"MEETING_STATUS_CONNECTING";
            break;
        case MobileRTCMeetingState_WaitingForHost:
            result = @"MEETING_STATUS_WAITINGFORHOST";
            break;
        case MobileRTCMeetingState_InMeeting:{
            result = @"MEETING_STATUS_INMEETING";
            //            [self getCurrentActiveId];
            [self connectAudio];
            [[GlobalData sharedInstance] setGlobalIsInMeeting:YES];
            break;
        }
        case MobileRTCMeetingState_Disconnecting:
            result = @"MEETING_STATUS_DISCONNECTING";
            [[GlobalData sharedInstance] setGlobalIsInMeeting:NO];
            break;
        case MobileRTCMeetingState_Reconnecting:
            result = @"MEETING_STATUS_RECONNECTING";
            break;
        case MobileRTCMeetingState_Failed:
            result = @"MEETING_STATUS_FAILED";
            break;
        case MobileRTCMeetingState_Ended: // only iOS (guessed naming)
            result = @"MEETING_STATUS_ENDED";
            break;
        case MobileRTCMeetingState_Locked: // only iOS (guessed naming)
            result = @"MEETING_STATUS_LOCKED";
            break;
        case MobileRTCMeetingState_Unlocked: // only iOS (guessed naming)
            result = @"MEETING_STATUS_UNLOCKED";
            break;
        case MobileRTCMeetingState_InWaitingRoom:
            result = @"MEETING_STATUS_IN_WAITING_ROOM";
            break;
        case MobileRTCMeetingState_WebinarPromote:
            result = @"MEETING_STATUS_WEBINAR_PROMOTE";
            break;
        case MobileRTCMeetingState_WebinarDePromote:
            result = @"MEETING_STATUS_WEBINAR_DEPROMOTE";
            break;
        case MobileRTCMeetingState_JoinBO: // only iOS (guessed naming)
            result = @"MEETING_STATUS_JOIN_BO";
            //            [self connectAudio];
            [[GlobalData sharedInstance] setGlobalIsConnectedDefaultAudioSession:NO];
            break;
        case MobileRTCMeetingState_LeaveBO: // only iOS (guessed naming)
            result = @"MEETING_STATUS_LEAVE_BO";
            //            [self connectAudio];
            [[GlobalData sharedInstance] setGlobalIsConnectedDefaultAudioSession:NO];
            break;
            
        default:
            [NSException raise:NSGenericException format:@"Unexpected state."];
    }
    
    return result;
}

- (void)getCurrentActiveId {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    BOOL isWebinarMeeting = [ms isWebinarMeeting];
    BOOL isViewingShare = [ms isViewingShare];
    if(isViewingShare) {
        [[GlobalData sharedInstance] setGlobalActiveShareID:[ms activeShareUserID]];
    } else {
        [[GlobalData sharedInstance] setUserID:[ms activeUserID]];
    }
}
- (void)onMeetingStateChange:(MobileRTCMeetingState)state {
    NSLog(@"onMeetingStatusChanged, meetingState=%@", @(state));
    
    NSString* statusString = [self formatStateToString:state];
    [self sendEventWithName:@"MeetingEvent" event:@"success" status:statusString];
    [self sendEventWithName:@"MeetingStatus" event:statusString];
    
    
    
    if (!enableCustomMeeting && state == MobileRTCMeetingState_Connecting && providerDelegate.callingUUID == nil) {
        NSUUID *callUUID = [NSUUID UUID];
        
        CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:callUUID handle:[[CXHandle alloc] initWithType:CXHandleTypeGeneric value:@"Học Viện Minh Trí Thành"]];
        CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
        callUpdate.remoteHandle = startCallAction.handle;
        callUpdate.hasVideo = startCallAction.video;
        CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];
        [callController requestTransaction:transaction completion:^(NSError * _Nullable error) {
            if (error) {
                NSLog(@"Error requesting start call transaction: %@", error.localizedDescription);
                self->providerDelegate.callingUUID = nil;
            } else {
                NSLog(@"Requested start call transaction succeeded");
                NSLog(@"callUUID 1: %@", callUUID);
                self->providerDelegate.callingUUID = callUUID;
                NSLog(@"callUUID 2: %@", self->providerDelegate.callingUUID);
            }
        }];
    }
    else if (!enableCustomMeeting && state == MobileRTCMeetingState_Ended && providerDelegate.callingUUID != nil) {
        NSLog(@"endCallUUID: %@", providerDelegate.callingUUID);
        CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:providerDelegate.callingUUID];
        CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
        [callController requestTransaction:transaction completion:^(NSError * _Nullable error) {
            if (error) {
                NSLog(@"Error requesting end call transaction: %@", error.localizedDescription);
            } else {
                NSLog(@"Requested end call transaction succeeded");
                self->providerDelegate.callingUUID = nil;
            }
        }];
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
    
    meetingPromiseResolve = nil;
    meetingPromiseReject = nil;
}

- (void)onMeetingEndedReason:(MobileRTCMeetingEndReason)reason {
    [self sendEventWithName:@"MeetingEvent" event:[self meetingEndReasonName:reason]];
}

- (void)onChatMessageNotification:(MobileRTCMeetingChat * _Nullable)chatInfo;
{
    if (!isInitialized) {
        return;
    }
    NSLog(@"RNZoomUsVideoView MobileRTCMeetingChat-->%@",chatInfo.content);
    NSDictionary *chatInfoDict = nil;
    if (chatInfo) {
        //            MobileRTCMeetingUserInfo *userInfo = [self getUserInfo:chatInfo.chatId];
        NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
        [dateFormatter setDateFormat:@"dd-MM-yyyy HH:mm:ss"];
        NSString *dateString = [dateFormatter stringFromDate:chatInfo.date];
        chatInfoDict = @{
            //                  @"avatar": userInfo.avatarPath,
            @"chatId": chatInfo.chatId,
            @"senderId": chatInfo.senderId,
            @"senderName": chatInfo.senderName,
            @"receiverId": chatInfo.receiverId,
            @"receiverName": chatInfo.receiverName,
            @"content": chatInfo.content,
            @"date": dateString,
            @"chatMessageType": @(chatInfo.chatMessageType),
            @"isMyself": @(chatInfo.isMyself),
            @"isPrivate": @(chatInfo.isPrivate),
            @"isChatToAll": @(chatInfo.isChatToAll),
            @"isChatToAllPanelist": @(chatInfo.isChatToAllPanelist),
            @"isChatToWaitingroom": @(chatInfo.isChatToWaitingroom),
            @"isComment": @(chatInfo.isComment),
            @"isThread": @(chatInfo.isThread),
            @"threadID": chatInfo.threadID,
            
        };
    }
    [self sendEventWithName:@"MeetingEvent" body: @{
        @"event": @"onChatMessageNotification",
        @"chatInfo": chatInfoDict
    }];
}
#pragma mark - Screen share functionality

- (void)onSinkShareSizeChange:(NSUInteger)userID {
    NSLog(@"RNZoomUs onSinkShareSizeChange==%@",@(userID));
}

- (void)onSinkMeetingActiveShare:(NSUInteger)userId {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    if (ms) {
        if (userId == 0) {
            [self sendEventWithName:@"MeetingEvent" event:@"screenShareStopped"];
        } else if ([ms isMyself:userId]){
            [self sendEventWithName:@"MeetingEvent" event:@"screenShareStarted"];
        } else {
            [self sendEventWithName:@"MeetingEvent" params:@{
                @"event": @"screenShareStartedByUser",
                @"userId": @(userId)
            }];
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

- (void)onSinkSharingStatus:(MobileRTCSSharingSourceInfo*_Nonnull)shareInfo {}

#pragma mark - https://marketplacefront.zoom.us/sdk/meeting/ios/_mobile_r_t_c_meeting_delegate_8h_source.html


#pragma mark - MobileRTCVideoServiceDelegate

- (void)onSinkMeetingVideoStatusChange:(NSUInteger)userID videoStatus:(MobileRTC_VideoStatus)videoStatus {
    NSLog(@"RNZoomUs onSinkMeetingVideoStatusChange=%@, videoStatus=%@",@(userID), @(videoStatus));
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    
    if (videoStatus == MobileRTC_VideoStatus_Video_ON) {
        [[GlobalData sharedInstance] setUserID:userID];
    }
    if ([ms isMyself:userID]) {
        [self sendEventWithName:@"MeetingEvent" event:@"myVideoStatusChanged" userInfo:[ms userInfoByID:[ms myselfUserID]]];
    }
}

-  (void)onSinkMeetingVideoRequestUnmuteByHost:(MobileRTCSDKError (^_Nonnull)(BOOL Accept))completion {
    [self sendEventWithName:@"MeetingEvent" event:@"askUnMuteVideo"];
    NSLog(@"RNZoomUs onSinkMeetingVideoRequestUnmuteByHost");
    if (completion)
    {
        MobileRTCSDKError err = completion(YES);
        NSLog(@"unmute accept %@", @(err));
    }
}

- (void)onVideoOrderUpdated:(NSArray <NSNumber *>* _Nullable)orderArr {}

- (void)onFollowHostVideoOrderChanged:(BOOL)follow {
    NSLog(@"[Video Order] callback onFollowHostVideoOrderChanged: %@", @(follow));
}

- (void)onSinkMeetingActiveVideo:(NSUInteger)userID {
    NSLog(@"RNZoomUs onSinkMeetingActiveVideo =>%@", @(userID));
    //    [[GlobalData sharedInstance] setUserID:userID];
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    BOOL isViewingShare = [ms isViewingShare];
    BOOL isWebinarMeeting = [ms isWebinarMeeting];
    if (isWebinarMeeting) [[GlobalData sharedInstance] setGlobalWebinarFirstActiveVideoID:userID];
    if (isViewingShare) [[GlobalData sharedInstance] setGlobalActiveShareID:userID];
    NSLog(@"RNZoomUs RNZoomUs isViewingShare =>%@", @(isViewingShare));
}

- (void)onSinkMeetingVideoStatusChange:(NSUInteger)userID {
    NSLog(@"RNZoomUs onSinkMeetingVideoStatusChange=%@",@(userID));
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    BOOL isWebinar = [ms isWebinarMeeting];
    BOOL isViewingShare = [ms isViewingShare];
    //    if (isViewingShare) [[GlobalData sharedInstance] setGlobalActiveShareID:userID];
}

- (void)onMyVideoStateChange {
    NSLog(@"RNZoomUs onMyVideoStateChange");
}

- (void)onSpotlightVideoChange:(BOOL)on {}

- (void)onSinkMeetingPreviewStopped {
    NSLog(@"RNZoomUs onSinkMeetingPreviewStopped");
}

- (void)onSinkMeetingActiveVideoForDeck:(NSUInteger)userID {
    NSLog(@"RNZoomUs onSinkMeetingActiveVideo =>%@", @(userID));
}

- (void)onSinkMeetingVideoQualityChanged:(MobileRTCNetworkQuality)qality userID:(NSUInteger)userID {
    NSLog(@"RNZoomUs onSinkMeetingVideoQualityChanged: %zd userID:%zd",qality,userID);
}

- (void)onSinkMeetingShowMinimizeMeetingOrBackZoomUI:(MobileRTCMinimizeMeetingState)state {
    switch (state) {
        case MobileRTCMinimizeMeeting_ShowMinimizeMeeting:
            [self sendEventWithName:@"MeetingEvent" body:@{@"event": @"showMinimizeMeeting", @"status": @"showMinimizeMeeting"}];
            break;
            
        case MobileRTCMinimizeMeeting_BackFullScreenMeeting:
            [self sendEventWithName:@"MeetingEvent" body:@{@"event": @"backFullScreenMeeting", @"status": @"backFullScreenMeeting"}];
            break;
        default:
            break;
    }
}

- (void)onBOOptionChanged:(MobileRTCBOOption *_Nonnull)newOption {}



- (void)onHostVideoOrderUpdated:(NSArray <NSNumber *>* _Nullable)orderArr;
{
    NSLog(@"[Video Order] callback onHostVideoOrderUpdated: %@", orderArr);
}

- (void)onLocalVideoOrderUpdated:(NSArray <NSNumber *>* _Nullable)localOrderArr
{
    NSLog(@"[Video Order] callback onLocalVideoOrderUpdated: %@", localOrderArr);
}

#pragma mark - MobileRTCAudioServiceDelegate

- (void)onSinkMeetingMyAudioTypeChange {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    [self sendEventWithName:@"MeetingEvent" event:@"myAudioTypeChanged" userInfo:[ms userInfoByID:[ms myselfUserID]]];
}

- (void)onSinkMeetingAudioStatusChange:(NSUInteger)userID audioStatus:(MobileRTC_AudioStatus)audioStatus {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    
    if ([ms isMyself:userID]) {
        [self sendEventWithName:@"MeetingEvent" event:@"myAudioStatusChanged" userInfo:[ms userInfoByID:[ms myselfUserID]]];
    }
}

// This looks like it doesnt get called check
// https://github.com/mieszko4/react-native-zoom-us/pull/144#issuecomment-931189245
- (void)onSinkMeetingAudioRequestUnmuteByHost {
    [self sendEventWithName:@"MeetingEvent" event:@"askUnMuteAudio"];
}

- (void)onSinkMeetingAudioStatusChange:(NSUInteger)userID {
    NSLog(@"onSinkMeetingAudioStatusChange");
}

- (void)onSinkMeetingAudioTypeChange:(NSUInteger)userID {}

- (void)onAudioOutputChange {}

- (void)onMyAudioStateChange {
    NSLog(@"onMyAudioStateChange");
}



#pragma mark - MobileRTCUserServiceDelegate

- (void)onMyHandStateChange {}

- (void)onInMeetingUserUpdated {}

- (void)onSinkMeetingUserRaiseHand:(NSUInteger)userID {}

- (void)onSinkMeetingUserLowerHand:(NSUInteger)userID {}

- (void)onSinkUserNameChanged:(NSArray <NSNumber*>* _Nullable)userNameChangedArr {}

- (void)onClaimHostResult:(MobileRTCClaimHostError)error {}

- (void)onMeetingHostChange:(NSUInteger)userId {
    [self sendEventWithName:@"MeetingEvent" params:@{
        @"event": @"hostChanged",
        @"userId": @(userId)
    }];
}

- (void)onMeetingCoHostChange:(NSUInteger)userId isCoHost:(BOOL)isCoHost {
    [self sendEventWithName:@"MeetingEvent" params:@{
        @"event": @"coHostChanged",
        @"userId": @(userId)
    }];
}

- (void)onSinkMeetingUserLeft:(NSUInteger)userID {
    NSLog(@"RNZoomUs onSinkMeetingUserLeft==%@", @(userID));
    [self sendEventWithName:@"MeetingEvent" params:@{
        @"event": @"userLeave",
        @"userList": @[@(userID)]
    }];
}
- (void)onSinkMeetingUserJoin:(NSUInteger)userID {
    NSLog(@"RNZoomUs onSinkMeetingUserJoin==%@", @(userID));
    
    [self sendEventWithName:@"MeetingEvent" params:@{
        @"event": @"userJoin",
        @"userList": @[@(userID)]
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
    return @[@"AuthEvent", @"MeetingEvent", @"MeetingStatus"];
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
- (void)sendEventWithName:(NSString *)name event:(NSString *)event userInfo:(MobileRTCMeetingUserInfo *)userInfo {
    if (hasObservers) {
        [self sendEventWithName:name body:@{
            @"event": event,
            @"userRole": [self getUserRole:[userInfo userRole]],
            @"audioType": @([[userInfo audioStatus] audioType]),
            @"isTalking": @([[userInfo audioStatus] isTalking]),
            @"isMutedAudio": @((BOOL)([[userInfo audioStatus] audioType] == 2 ? YES : [[userInfo audioStatus] isMuted])),
            @"isMutedVideo": @((BOOL)![[userInfo videoStatus] isSending]),
        }];
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
        case MobileRTCAuthError_LimitExceededException : return @"limitExceeded";
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
        case MobileRTCMeetError_UnableToJoinExternalMeeting: return @"unableToJoinExternalMeeting";
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

- (BOOL)onCheckIfMeetingVoIPCallRunning{
    return [providerDelegate isInCall];
}
- (void)onStartBOError:(MobileRTCBOControllerError)errType {
    
    [self sendEventWithName:@"onStartBOError" event:[self startBOErrorName:errType]];
}


- (NSString *)startBOErrorName:(MobileRTCBOControllerError)error {
    switch (error) {
        case MobileRTCBOControllerError_NULL_POINTER: return @"NULL_POINTER";
        case MobileRTCBOControllerError_WRONG_CURRENT_STATUS: return @"WRONG_CURRENT_STATUS";
        case MobileRTCBOControllerError_TOKEN_NOT_READY: return @"TOKEN_NOT_READY";
        case MobileRTCBOControllerError_NO_PRIVILEGE: return @"NO_PRIVILEGE";
        case MobileRTCBOControllerError_BO_LIST_IS_UPLOADING: return @"BO_LIST_IS_UPLOADING";
        case MobileRTCBOControllerError_UPLOAD_FAIL: return @"UPLOAD_FAIL";
        case MobileRTCBOControllerError_NO_ONE_HAS_BEEN_ASSIGNED: return @"NO_ONE_HAS_BEEN_ASSIGNED";
        case MobileRTCBOControllerError_UNKNOWN: return @"UNKNOWN";
        default: return @"unknown";
    }
}

@end
