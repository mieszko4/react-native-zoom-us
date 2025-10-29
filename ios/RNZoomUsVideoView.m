//
//  RNZoomUsVideoView.m
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/11.
//
//

#import "RNZoomUsVideoView.h"
#import "RNZoomUs.h"
#import "RCTEventEmitter.h"
#import "GlobalData.h"
@implementation RNZoomUsVideoView
BOOL hasObservers;
ProviderDelegate *providerDelegate;
CXCallController *callController;

- (instancetype)init
{
    self = [super init];
    //    providerDelegate = nil;
    //    callController = nil;
    providerDelegate = [[ProviderDelegate alloc] init];
    callController = [[CXCallController alloc] init];
    self.lastVideoStatusByUserID = [NSMutableDictionary dictionary];
    _rnZoomUsVideoViewController = [[CustomMeetingViewController alloc] init];
    _rnZoomUsVideoViewController.view.frame = self.bounds;
    [self addSubview:_rnZoomUsVideoViewController.view];
    if (self) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(audioRouteChanged:)
                                                     name:AVAudioSessionRouteChangeNotification
                                                   object:nil];
    }
    return self;
}

- (void)reactSetFrame:(CGRect)frame {
    [super reactSetFrame:frame];
}

- (void)dealloc
{
    NSLog(@"RNZoomUsVideoView onAICompanionFeatureCanNotBeTurnedOff");
    [[NSNotificationCenter defaultCenter] removeObserver:self
                                                    name:AVAudioSessionRouteChangeNotification
                                                  object:nil];
    NSLog(@"Cleaned up RNZoomUsVideoView");
}
- (void)audioRouteChanged:(NSNotification *)notification {
    NSDictionary *userInfo = notification.userInfo;
    AVAudioSessionRouteChangeReason reason = [userInfo[AVAudioSessionRouteChangeReasonKey] unsignedIntegerValue];
    
    if (reason == AVAudioSessionRouteChangeReasonNewDeviceAvailable) {
        NSLog(@"RNZoomUsVideoView Headphones connected");
    } else if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
        NSLog(@"RNZoomUsVideoView Headphones disconnected");
    }
    [self updateAudioOutput];
}
- (void)updateAudioOutput {
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    NSError *error = nil;
    
    [audioSession setActive:YES error:&error];
    if (error) {
        NSLog(@"RNZoomUsVideoView Error activating audio session: %@", error.localizedDescription);
        return;
    }
    
    AVAudioSessionRouteDescription *currentRoute = audioSession.currentRoute;
    BOOL isHeadphonesConnected = NO;
    for (AVAudioSessionPortDescription *output in currentRoute.outputs) {
        NSLog(@"RNZoomUsVideoView PortType: %@", output.portType );
        NSArray *headphoneTypes = @[AVAudioSessionPortHeadphones,
                                    AVAudioSessionPortBluetoothHFP,
                                    AVAudioSessionPortBluetoothLE,
                                    AVAudioSessionPortBluetoothA2DP];
        
        if ([headphoneTypes containsObject:output.portType]) {
            isHeadphonesConnected = YES;
            break;
        }
    }
    
    if (!isHeadphonesConnected) {
        [audioSession overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:&error];
        if (error) {
            BOOL isInmeeting = [[GlobalData sharedInstance] globalIsInMeeting];
//            if (isInmeeting) {
//                [self connectAudio];
//            }
            NSLog(@"RNZoomUsVideoView Error overriding to speaker: %@", error.localizedDescription);
        }
    }
}

- (void)layoutSubviews
{
    [super layoutSubviews];
    
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    
    if (ms) {
        ms.delegate = self;
        [self addViewControllerAsSubView];
        if (_rnZoomUsVideoViewController != nil) {
            _rnZoomUsVideoViewController.view.frame = self.bounds;
        }
    }
}

- (void)removeFromSuperview {
    if (_rnZoomUsVideoViewController != nil) {
        [_rnZoomUsVideoViewController willMoveToParentViewController:nil];
        [_rnZoomUsVideoViewController.view removeFromSuperview];
        [_rnZoomUsVideoViewController removeFromParentViewController];
        _rnZoomUsVideoViewController = nil;
        [super removeFromSuperview];
    }
}

-(void)addViewControllerAsSubView
{
}

- (void)connectAudio {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    MobileRTCMeetingSettings *zoomSettings = [[MobileRTC sharedRTC] getMeetingSettings];
    
    if (!ms) return;
    [ms connectMyAudio: YES];
//    [ms resetMeetingAudioSession];
    //    [ms muteMyAudio: YES];
    //    [ms muteMyVideo: YES];
    NSLog(@"connectAudio");
}

- (void)resetAudioSession {
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    NSError *error = nil;
    
    // Remove any audio port override
    [audioSession overrideOutputAudioPort:AVAudioSessionPortOverrideNone error:&error];
    if (error) {
        NSLog(@"Error removing audio port override: %@", error.localizedDescription);
    }
    
    // Optionally reset the category and mode (can be customized based on your app needs)
    [audioSession setCategory:AVAudioSessionCategorySoloAmbient error:&error];
    if (error) {
        NSLog(@"Error setting default category: %@", error.localizedDescription);
    }
    
    [audioSession setMode:AVAudioSessionModeDefault error:&error];
    if (error) {
        NSLog(@"Error setting default mode: %@", error.localizedDescription);
    }
    
    // Deactivate the audio session
    [audioSession setActive:NO error:&error];
    if (error) {
        NSLog(@"Error deactivating audio session: %@", error.localizedDescription);
    }
}


#pragma mark - Meeting Service Delegate
- (void)onMeetingStateChange:(MobileRTCMeetingState)state{
    NSLog(@"RNZoomUsVideoView onMeetingStateChange =>%@", @(state));
    NSString* result;
    switch(state) {
        case MobileRTCMeetingState_Idle:
            result = @"MEETING_STATUS_IDLE";
            break;
        case MobileRTCMeetingState_Connecting:
            result = @"MEETING_STATUS_CONNECTING";
//            if (providerDelegate.callingUUID == nil) {
//                NSUUID *callUUID = [NSUUID UUID];
//                
//                CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:callUUID handle:[[CXHandle alloc] initWithType:CXHandleTypeGeneric value:@"Học Viện Minh Trí Thành"]];
//                CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
//                callUpdate.remoteHandle = startCallAction.handle;
//                callUpdate.hasVideo = startCallAction.video;
//                CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];
//                [callController requestTransaction:transaction completion:^(NSError * _Nullable error) {
//                    if (error) {
//                        NSLog(@"Error requesting start call transaction: %@", error.localizedDescription);
//                        providerDelegate.callingUUID = nil;
//                    } else {
//                        NSLog(@"Requested start call transaction succeeded");
//                        NSLog(@"callUUID 1: %@", callUUID);
//                        providerDelegate.callingUUID = callUUID;
//                        NSLog(@"callUUID 2: %@", providerDelegate.callingUUID);
//                    }
//                }];
//            }
            break;
        case MobileRTCMeetingState_WaitingForHost:
            result = @"MEETING_STATUS_WAITINGFORHOST";
            break;
        case MobileRTCMeetingState_InMeeting:
            result = @"MEETING_STATUS_INMEETING";
            [self connectAudio];
            break;
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
            if (providerDelegate.callingUUID != nil) {
                NSLog(@"endCallUUID: %@", providerDelegate.callingUUID);
                CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:providerDelegate.callingUUID];
                CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
                [callController requestTransaction:transaction completion:^(NSError * _Nullable error) {
                    if (error) {
                        NSLog(@"Error requesting end call transaction: %@", error.localizedDescription);
                    } else {
                        NSLog(@"Requested end call transaction succeeded");
                        providerDelegate.callingUUID = nil;
                    }
                }];
            }
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
            break;
        case MobileRTCMeetingState_LeaveBO: // only iOS (guessed naming)
            result = @"MEETING_STATUS_LEAVE_BO";
            break;
            
        default:
            [NSException raise:NSGenericException format:@"Unexpected state."];
    }
    
    NSLog(@"RNZoomUsVideoView onMeetingStateChange =>%@", result);
    
    
    if (self.onMeetingStateChange) {
        self.onMeetingStateChange(@{
            @"event": @"success",
            @"status": result
        });
    }
}

- (void)onSinkMeetingActiveVideo:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkMeetingActiveVideo =>>%@", @(userID));
    if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onSinkMeetingActiveVideo:)])
    {
        [_rnZoomUsVideoViewController onSinkMeetingActiveVideo:userID];
    }
}

- (void)onMyVideoStateChange {
    NSLog(@"RNZoomUsVideoView onMyVideoStateChange");
    if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onMyVideoStateChange)])
    {
        [_rnZoomUsVideoViewController onMyVideoStateChange];
    }
}

- (void)onSpotlightVideoChange:(BOOL)on {}

- (void)onSinkMeetingPreviewStopped {
    NSLog(@"RNZoomUsVideoView onSinkMeetingPreviewStopped");
    if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onSinkMeetingPreviewStopped)])
    {
        [_rnZoomUsVideoViewController onSinkMeetingPreviewStopped];
    }
    if (self.onMeetingPreviewStopped) {
        self.onMeetingPreviewStopped(@{
            @"event": @"onMeetingPreviewStopped",
            @"status": @(NO)
        });
    }
}

- (void)onSinkMeetingVideoStatusChange:(NSUInteger)userID videoStatus:(MobileRTC_VideoStatus)videoStatus{
    NSLog(@"RNZoomUsVideoView onSinkMeetingVideoStatusChange=%@, videoStatus=%@",@(userID), @(videoStatus));
    
    NSNumber *lastStatus = self.lastVideoStatusByUserID[@(userID)];
    if (lastStatus && [lastStatus integerValue] == videoStatus) {
        return;
    }
    self.lastVideoStatusByUserID[@(userID)] = @(videoStatus);
    
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    BOOL isHostUser = [ms isHostUser:userID];
    BOOL isWebinarMeeting = [ms isWebinarMeeting];
    if (videoStatus == MobileRTC_VideoStatus_Video_ON) {
        
        //        [[GlobalData sharedInstance] setUserID:userID];
        if (isWebinarMeeting && !isHostUser) {
            return;
        }
        if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onSinkMeetingVideoStatusChange:)])
        {
            [_rnZoomUsVideoViewController onSinkMeetingVideoStatusChange:userID];
        }
    }
    if (videoStatus == MobileRTC_VideoStatus_Video_OFF) {
        //        if (isWebinarMeeting) {
        //            NSUInteger globalWebinarFirstActiveVideoID = [[GlobalData sharedInstance] globalWebinarFirstActiveVideoID];
        //            if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onSinkMeetingVideoStatusChange:)])
        //            {
        //                [_rnZoomUsVideoViewController onSinkMeetingVideoStatusChange:globalWebinarFirstActiveVideoID];
        //            }
        //        }
    }
    if (self.onSinkMeetingVideoStatusChange) {
        self.onSinkMeetingVideoStatusChange(@{
            @"event": @"onSinkMeetingVideoStatusChange",
            @"videoStatus": @(videoStatus)
        });
    }
}

- (void)onSinkMeetingActiveVideoForDeck:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkMeetingActiveVideoForDeck =>%@", @(userID));
    if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onSinkMeetingActiveVideo:)])
    {
        [_rnZoomUsVideoViewController onSinkMeetingActiveVideo:userID];
    }
}

- (void)onSinkMeetingUserLeft:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkMeetingUserLeft==%@", @(userID));
    if (self.onSinkMeetingUserLeft) {
        self.onSinkMeetingUserLeft(@{
            @"event": @"userLeave",
            @"userList": @[@(userID)]
        });
    }
    [self countInMeetingUser];
}
- (void)onSinkMeetingUserJoin:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkMeetingUserJoin==%@", @(userID));
    if (self.onSinkMeetingUserJoin) {
        self.onSinkMeetingUserJoin(@{
            @"event": @"userJoin",
            @"userList": @[@(userID)]
        });
    }
    [self countInMeetingUser];
}

- (void)onSinkMeetingAudioRequestUnmuteByHost {
    if (self.onMeetingAudioRequestUnmuteByHost) {
        self.onMeetingAudioRequestUnmuteByHost(@{
            @"event": @"REQUEST_AUDIO",
            @"status": @(YES)
        });
    }
}

- (void)onSinkMeetingVideoRequestUnmuteByHost:(MobileRTCSDKError (^)(BOOL))completion {
    if (self.onMeetingVideoRequestUnmuteByHost) {
        self.onMeetingVideoRequestUnmuteByHost(@{
            @"event": @"REQUEST_VIDEO",
            @"status": @(YES)
        });
    }
}
- (void)onSinkMeetingAudioStatusChange:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkMeetingAudioStatusChange=%@",@(userID));
}
- (void)onSinkMeetingAudioStatusChange:(NSUInteger)userID audioStatus:(MobileRTC_AudioStatus)audioStatus {
    NSLog(@"RNZoomUsVideoView onSinkMeetingAudioStatusChange=%@, audioStatus=%@",@(userID), @(audioStatus));
    
    if (self.onSinkMeetingAudioStatusChange) {
        self.onSinkMeetingAudioStatusChange(@{
            @"event": @"onSinkMeetingAudioStatusChange",
            @"audioStatus": @(audioStatus)
        });
    }
}

#pragma mark - In meeting users' state updated
- (void)onInMeetingUserUpdated
{
    //    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    //    NSArray *users = [ms getInMeetingUserList];
    //    NSLog(@"RNZoomUsVideoView onInMeetingUserUpdated:%@", users);
    [self countInMeetingUser];
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

- (void)onInMeetingUserAvatarPathUpdated:(NSInteger)userID {
    [self getUserInfo:userID];
}

-(MobileRTCMeetingUserInfo* _Nullable)getUserInfo:(NSInteger)userID {
    NSLog(@"onInMeetingUserAvatarPathUpdated --- %s %ld",__FUNCTION__,userID);
    MobileRTCMeetingUserInfo *userInfo = [[[MobileRTC sharedRTC] getMeetingService] userInfoByID:userID];
    NSLog(@"onInMeetingUserAvatarPathUpdated --- userInfo avatarPath:%@",userInfo.avatarPath);
    return userInfo;
}

- (void)onChatMessageNotification:(MobileRTCMeetingChat * _Nullable)chatInfo;
{
    NSLog(@"RNZoomUsVideoView MobileRTCMeetingChat-->%@",chatInfo.content);
    if (self.onChatMessageNotification) {
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
        self.onChatMessageNotification(@{
            @"event": @"onChatMessageNotification",
            @"chatInfo": chatInfoDict
        });
    }
}

- (void)onChatMsgDeleteNotification:(NSString *_Nonnull)msgID deleteBy:(MobileRTCChatMessageDeleteType)deleteBy
{
    NSLog(@"RNZoomUsVideoView onChatMsgDeleteNotification-->%@ deleteBy-->%@",msgID,@(deleteBy));
    
    if (self.onChatMsgDeleteNotification) {
        NSDictionary *chatInfoDict = nil;
        chatInfoDict = @{
            @"deleteBy": @(deleteBy),
            @"msgID": msgID,
        };
        self.onChatMsgDeleteNotification(@{
            @"event": @"onChatMsgDeleteNotification",
            @"msgDelete": chatInfoDict
        });
    }
}

- (void)onSinkUserNameChanged:(NSArray <NSNumber *>* _Nullable)userNameChangedArr
{
    NSLog(@"onSinkUserNameChanged:%@", userNameChangedArr);
}

- (void)onSinkMeetingUserRaiseHand:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkMeetingUserRaiseHand==%@", @(userID));
}

- (void)onSinkMeetingUserLowerHand:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkMeetingUserLowerHand==%@", @(userID));
}

- (void)onMeetingHostChange:(NSUInteger)hostId {
    NSLog(@"RNZoomUsVideoView onMeetingHostChange==%@", @(hostId));
}

- (void)onMeetingCoHostChange:(NSUInteger)userID isCoHost:(BOOL)isCoHost {
    NSLog(@"RNZoomUsVideoView onMeetingCoHostChange==%@ isCoHost===%@", @(userID), @(isCoHost));
}

- (void)onClaimHostResult:(MobileRTCClaimHostError)error
{
    NSLog(@"RNZoomUsVideoView onClaimHostResult==%@", @(error));
}

#pragma mark - BO MEETING

- (void)onHasCreatorRightsNotification:(MobileRTCBOCreator *_Nonnull)creator
{
    NSLog(@"RNZoomUsVideoView Own Creator");
}

- (void)onHasAdminRightsNotification:(MobileRTCBOAdmin * _Nonnull)admin
{
    NSLog(@"RNZoomUsVideoView Own Admin");
}

- (void)onHasAssistantRightsNotification:(MobileRTCBOAssistant * _Nonnull)assistant
{
    NSLog(@"RNZoomUsVideoView Own Assistant");
}

- (void)onHasAttendeeRightsNotification:(MobileRTCBOAttendee * _Nonnull)attendee
{
    NSString *boName = [attendee getBOName];
    
    if (self.onHasAttendeeRightsNotification) {
        self.onHasAttendeeRightsNotification(@{
            @"event": @"onHasAttendeeRightsNotification",
            @"boName": boName
        });
    }
    NSLog(@"RNZoomUsVideoView Own Attendee");
}

- (void)onHasDataHelperRightsNotification:(MobileRTCBOData * _Nonnull)dataHelper
{
    NSLog(@"RNZoomUsVideoView Own Data Helper");
}

- (void)onLostCreatorRightsNotification
{
    NSLog(@"RNZoomUsVideoView Lost Creator");
}

- (void)onLostAdminRightsNotification;
{
    NSLog(@"RNZoomUsVideoView Lost Admin");
}

- (void)onLostAssistantRightsNotification
{
    NSLog(@"RNZoomUsVideoView Lost Assistant");
}

- (void)onLostAttendeeRightsNotification
{
    NSLog(@"RNZoomUsVideoView Lost Attendee");
}

- (void)onNewBroadcastMessageReceived:(NSString *_Nullable)broadcastMsg senderID:(NSUInteger)senderID {
    NSLog(@"RNZoomUsVideoView Broadcast Message Received:%@ senderID:%@", broadcastMsg, @(senderID));
}

- (void)onBOStopCountDown:(NSUInteger)seconds
{
    NSLog(@"RNZoomUsVideoView onBOStopCountDown:%@", @(seconds));
}

- (void)onHostInviteReturnToMainSession:(NSString *_Nullable)hostName replyHandler:(MobileRTCReturnToMainSessionHandler *_Nullable)replyHandler
{
    NSLog(@"RNZoomUsVideoView onHostInviteReturnToMainSession hostName=:%@, replyHandler=:%p", hostName, replyHandler);
}

- (void)onBOStatusChanged:(MobileRTCBOStatus)status
{
    NSLog(@"RNZoomUsVideoView onBOStatusChanged status=:%@", @(status));
    NSString* result;
    switch(status) {
        case MobileRTCBOStatus_Invalid:
            result = @"MobileRTCBOStatus_Invalid";
            break;
        case MobileRTCBOStatus_Edit:
            result = @"MobileRTCBOStatus_Edit";
            break;
        case MobileRTCBOStatus_Started:
            result = @"MobileRTCBOStatus_Started";
            break;
        case MobileRTCBOStatus_Stopping:
            result = @"MobileRTCBOStatus_Stopping";
            break;
        case MobileRTCBOStatus_Ended:
            result = @"MobileRTCBOStatus_Ended";
            break;
        default:
            [NSException raise:NSGenericException format:@"Unexpected state."];
    }
    if (self.onBOStatusChanged) {
        self.onBOStatusChanged(@{
            @"event": @"success",
            @"status": result
        });
    }
}

- (void)onBOSwitchRequestReceived:(NSString*)newBOName newBOID:(NSString*)newBOID {
    NSLog(@"RNZoomUsVideoView onBOSwitchRequestReceived newBOName=:%@ newBOID=:%@", newBOName, newBOID);
}

- (void)onLostDataHelperRightsNotification
{
    NSLog(@"RNZoomUsVideoView Lost DataHelper");
}

- (void)onHelpRequestReceived:(NSString *_Nullable)strUserID {
    NSLog(@"RNZoomUsVideoView help request received from %@", strUserID);
}

- (void)onHelpRequestHandleResultReceived:(MobileRTCBOHelpReply)eResult {
    NSString *replyStatus = @"";
    switch (eResult) {
        case MobileRTCBOHelpReply_Idle: {
            replyStatus = @"Idle";
        } break;
        case MobileRTCBOHelpReply_Busy: {
            replyStatus = @"Busy";
        } break;
        case MobileRTCBOHelpReply_Ignore: {
            replyStatus = @"Ignore";
        } break;
        case MobileRTCBOHelpReply_alreadyInBO: {
            replyStatus = @"alreadyInBO";
        } break;
        default: break;
    }
    NSLog(@"RNZoomUsVideoView help request replied: %@", replyStatus);
}

- (void)onHostJoinedThisBOMeeting {
    NSLog(@"RNZoomUsVideoView Host has joined this BO");
}

- (void)onHostLeaveThisBOMeeting {
    NSLog(@"RNZoomUsVideoView Host has left this BO");
}

- (void)onBOInfoUpdated:(NSString *_Nullable)boId;
{
    NSLog(@"RNZoomUsVideoView BO info updated");
}

- (void)onUnAssignedUserUpdated
{
    NSLog(@"RNZoomUsVideoView un-assigned user updated");
}

- (void)onBOListInfoUpdated
{
    NSLog(@"RNZoomUsVideoView onBOListInfoUpdated");
}

- (void)onStartBOError:(MobileRTCBOControllerError)errType {
    NSLog(@"RNZoomUsVideoView admin start bo error: %@", @(errType));
}

- (void)onBOEndTimerUpdated:(NSUInteger)remaining isTimesUpNotice:(BOOL)isTimesUpNotice {
    NSLog(@"RNZoomUsVideoView admin bo %lu seconds left, isTimesUpNotice: %@", remaining, isTimesUpNotice ? @"Y" : @"N");
}

- (void)onBOCreateSuccess:(NSString *_Nullable)BOID {
    NSLog(@"RNZoomUsVideoView creator create success ret bo_id: %@", BOID);
}

- (void)onWebPreAssignBODataDownloadStatusChanged:(MobileRTCBOPreAssignBODataStatus)status {
    NSLog(@"RNZoomUsVideoView onWebPreAssignBODataDownloadStatusChanged: %@", @(status));
}

#pragma mark - SHARING

- (void)onSharingContentStartReceiving {
    NSLog(@"RNZoomUsVideoView onSharingContentStartReceiving");
}

- (void)onSinkSharingStatus:(MobileRTCSSharingSourceInfo*_Nonnull)shareInfo
{
    MobileRTCSharingStatus status = [shareInfo getStatus];
    NSInteger userID = [shareInfo getUserID];
    NSInteger shareSourceID = [shareInfo getShareSourceID];
    NSLog(@"--- %s status:%@",__FUNCTION__,shareInfo.description);
    NSLog(@"RNZoomUsVideoView onSinkSharingStatus==%@ userID==%@", @(status),@(userID));
    
    
    switch (status) {
        case MobileRTCSharingStatus_Other_Share_Begin:
            [[GlobalData sharedInstance] setGlobalActiveShareID:userID];
            break;
            
        default:
            break;
    }
    if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onSinkSharingStatus:userID:)])
    {
        [_rnZoomUsVideoViewController onSinkSharingStatus:shareInfo];
    }
}

- (void)onSinkShareSizeChange:(NSUInteger)userID {
    NSLog(@"RNZoomUsVideoView onSinkShareSizeChange==%@",@(userID));
    if (_rnZoomUsVideoViewController && [_rnZoomUsVideoViewController respondsToSelector:@selector(onSinkShareSizeChange:)])
    {
        [_rnZoomUsVideoViewController onSinkShareSizeChange: userID];
    }
}

- (void)onAppShareSplash{
    NSLog(@"RNZoomUsVideoView onAppShareSplash");
}

- (void)onAudioOutputChange {
    NSLog(@"RNZoomUsVideoView onAudioOutputChange");
}

- (void)onActionBeforeDestroyed:(NSUInteger)sharingID {
    NSLog(@"RNZoomUsVideoView onActionBeforeDestroyed==%@",@(sharingID));
}

- (void)onAllowAskQuestionStatus:(BOOL)bEnabled {
    NSLog(@"RNZoomUsVideoView onAllowAskQuestionStatus==%@",@(bEnabled));
}

- (void)onAskToEndOtherMeeting:(void (^)(BOOL))completion {
    NSLog(@"RNZoomUsVideoView onAskToEndOtherMeeting");
}

- (void)onAvailableLanguageListUpdated:(NSArray<MobileRTCInterpretationLanguage *> *)availableLanguageList {
    NSLog(@"RNZoomUsVideoView onAvailableLanguageListUpdated");
}

- (void)onAllowParticipantsRenameNotification:(BOOL)allow {
    NSLog(@"RNZoomUsVideoView onAllowParticipantsRenameNotification==%@",@(allow));
}
- (void)onAICompanionActiveChangeNotice:(BOOL)isActive {
    NSLog(@"RNZoomUsVideoView onAICompanionActiveChangeNotice==%@",@(isActive));
}

- (void)onAvailableSignLanguageListUpdated:(NSArray<MobileRTCSignInterpreterLanguage *> *)availableSignLanguageList {
    NSLog(@"RNZoomUsVideoView onAvailableSignLanguageListUpdated");
}
- (void)onAICompanionFeatureSwitchRequested:(MobileRTCAICompanionSwitchHandler *)handler {
    NSLog(@"RNZoomUsVideoView onAICompanionFeatureSwitchRequested");
}

- (void)onAllowWebinarReactionStatusChanged:(BOOL)canReaction {
    NSLog(@"RNZoomUsVideoView onAllowWebinarReactionStatusChanged");
}

- (void)onAllowParticipantsRequestCloudRecording:(BOOL)allow {
    NSLog(@"RNZoomUsVideoView onAllowParticipantsRequestCloudRecording");
}

- (void)onAllowParticipantsStartVideoNotification:(BOOL)allow {
    NSLog(@"RNZoomUsVideoView onAllowParticipantsStartVideoNotification");
}

- (void)onAllowParticipantsUnmuteSelfNotification:(BOOL)allow {
    NSLog(@"RNZoomUsVideoView onAllowParticipantsUnmuteSelfNotification");
}

- (void)onAllowParticipantsShareStatusNotification:(BOOL)allow {
    NSLog(@"RNZoomUsVideoView onAllowParticipantsShareStatusNotification");
}

- (void)onAllowAttendeeRaiseHandStatusChanged:(BOOL)canRaiseHand {
    NSLog(@"RNZoomUsVideoView onAllowAttendeeRaiseHandStatusChanged");
}

- (void)onAnnotationService:(MobileRTCAnnotationService *)service supportStatusChanged:(BOOL)support {
    NSLog(@"RNZoomUsVideoView onAnnotationService");
}

- (void)onAllowParticipantsShareWhiteBoardNotification:(BOOL)allow {
    NSLog(@"RNZoomUsVideoView onAllowParticipantsShareWhiteBoardNotification");
}

- (void)onAICompanionFeatureTurnOffByParticipant:(MobileRTCAICompanionTurnOnAgainHandler *)handler {
    NSLog(@"RNZoomUsVideoView onAICompanionFeatureTurnOffByParticipant");
}

- (void)onAICompanionFeatureCanNotBeTurnedOff:(NSArray<NSNumber *> *)featuresArr {
    NSLog(@"RNZoomUsVideoView onAICompanionFeatureCanNotBeTurnedOff");
}

- (void)onAllowAttendeeViewTheParticipantCountStatusChanged:(BOOL)canViewParticipantCount {
    NSLog(@"RNZoomUsVideoView onAllowAttendeeViewTheParticipantCountStatusChanged");
}
- (void)onAICompanionFeatureSwitchRequestResponse:(BOOL)timeout agree:(BOOL)agree turn:(BOOL)turnOn {
    NSLog(@"RNZoomUsVideoView onAICompanionFeatureSwitchRequestResponse");
    
}
- (void)onBOOptionChanged:(MobileRTCBOOption *)newOption {
    NSLog(@"RNZoomUsVideoView onBOOptionChanged");
}
- (void)onBroadcastBOVoiceStatus:(BOOL)bStart {
    NSLog(@"RNZoomUsVideoView onBroadcastBOVoiceStatus");
}
- (void)onCameraNoPrivilege {
    NSLog(@"RNZoomUsVideoView onCameraNoPrivilege");
}
- (void)onClickShareScreen:(UIViewController *)parentVC {
    NSLog(@"RNZoomUsVideoView onClickShareScreen");
}
- (void)onCheckCMRPrivilege:(MobileRTCCMRError)result {
    NSLog(@"RNZoomUsVideoView onCheckCMRPrivilege");
}
- (void)onCaptionStatusChanged:(BOOL)enable {
    NSLog(@"RNZoomUsVideoView onCaptionStatusChanged");
}

- (void)onCloudRecordingStorageFull:(long)gracePeriodDate {
    NSLog(@"RNZoomUsVideoView onCloudRecordingStorageFull");
}

- (void)onCallRoomDeviceStateChanged:(H323CallOutStatus)state {
    NSLog(@"RNZoomUsVideoView onCallRoomDeviceStateChanged");
}
- (void)onCustomWaitingRoomDataUpdated:(MobileRTCCustomWaitingRoomData *)data {
    NSLog(@"RNZoomUsVideoView onCustomWaitingRoomDataUpdated");
}

- (void)onClickedDialOut:(UIViewController *)parentVC isCallMe:(BOOL)me {
    NSLog(@"RNZoomUsVideoView onClickedDialOut");
}

- (void)onClosedCaptionReceived:(NSString *)message speakerId:(NSUInteger)speakerID msgTime:(NSDate *)msgTime {
    NSLog(@"RNZoomUsVideoView onClosedCaptionReceived");
}
- (void)onDialOutStatusChanged:(DialOutStatus)status {
    NSLog(@"RNZoomUsVideoView onDialOutStatusChanged");
}
- (void)onEmojiFeedbackCanceled:(NSUInteger)userId {
    NSLog(@"RNZoomUsVideoView onEmojiFeedbackCanceled");
}
- (void)onE2EEMeetingSecurityCodeChanged {
    NSLog(@"RNZoomUsVideoView onE2EEMeetingSecurityCodeChanged");
}
- (void)onEmojiReactionReceivedInWebinar:(MobileRTCEmojiReactionType)type{
    NSLog(@"RNZoomUsVideoView onEmojiReactionReceivedInWebinar");
}
- (void)onEmojiFeedbackReceived:(NSUInteger)userId feedbackType:(MobileRTCEmojiFeedbackType)type {
    NSLog(@"RNZoomUsVideoView onEmojiFeedbackReceived");
}

- (void)onEmojiReactionReceived:(NSUInteger)userId reactionType:(MobileRTCEmojiReactionType)type reactionSkinTone:(MobileRTCEmojiReactionSkinTone)skinTone {
    NSLog(@"RNZoomUsVideoView onEmojiReactionReceived");
}

- (void)onFileReceived:(MobileRTCFileReceiver *)receiver{
    NSLog(@"RNZoomUsVideoView onFileReceived");
}
- (void)onFileSendStart:(MobileRTCFileSender *)sender{
    NSLog(@"RNZoomUsVideoView onFileSendStart");
}
- (void)onFileTransferProgress:(MobileRTCFileTransferInfo *)info{
    NSLog(@"RNZoomUsVideoView onFileTransferProgress");
}
- (void)onFocusModeStateChanged:(BOOL)on{
    NSLog(@"RNZoomUsVideoView onFocusModeStateChanged");
}
- (void)onFocusModeShareTypeChanged:(MobileRTCFocusModeShareType)shareType{
    NSLog(@"RNZoomUsVideoView onFocusModeShareTypeChanged");
}
- (void)onFollowHostVideoOrderChanged:(BOOL)follow {
    NSLog(@"RNZoomUsVideoView onFollowHostVideoOrderChanged");
}
- (void)onFreeMeetingUpgradedToProMeeting {
    NSLog(@"RNZoomUsVideoView onFreeMeetingUpgradedToProMeeting");
}
- (void)onFreeMeetingUpgradeToGiftFreeTrialStop {
    NSLog(@"RNZoomUsVideoView onFreeMeetingUpgradeToGiftFreeTrialStop");
}
- (void)onFreeMeetingUpgradeToGiftFreeTrialStart {
    NSLog(@"RNZoomUsVideoView onFreeMeetingUpgradeToGiftFreeTrialStart");
}
- (void)onFreeMeetingNeedToUpgrade:(FreeMeetingNeedUpgradeType)type giftUpgradeURL:(NSString *)giftURL {
    NSLog(@"RNZoomUsVideoView onFreeMeetingNeedToUpgrade");
}
- (void)onGetRightAnswerListPrivilege:(BOOL)bCan {
    NSLog(@"RNZoomUsVideoView onGetRightAnswerListPrivilege");
}
- (void)onHostVideoOrderUpdated:(NSArray<NSNumber *> *)orderArr {
    NSLog(@"RNZoomUsVideoView onHostVideoOrderUpdated");
}
- (void)onInterpretationStop{
    NSLog(@"RNZoomUsVideoView onInterpretationStop");
}
- (void)onInterpretationStart{
    NSLog(@"RNZoomUsVideoView onInterpretationStart");
}
- (void)onInterpreterListChanged{
    NSLog(@"RNZoomUsVideoView onInterpreterListChanged");
}
- (void)onInterpreterLanguagesUpdated:(NSArray<MobileRTCInterpretationLanguage *> *)availableLanguages{
    NSLog(@"RNZoomUsVideoView onInterpreterLanguagesUpdated");
}
- (void)onInterpreterRoleChanged:(NSUInteger)userID isInterpreter:(BOOL)isInterpreter{
    NSLog(@"RNZoomUsVideoView onInterpreterRoleChanged");
}
- (void)onInterpreterLanguageChanged:(NSInteger)lanID1 andLanguage2:(NSInteger)lanID2{
    NSLog(@"RNZoomUsVideoView onInterpreterLanguageChanged");
}
- (void)onInterpreterActiveLanguageChanged:(NSInteger)userID activeLanguageId:(NSInteger)activeLanID{
    NSLog(@"RNZoomUsVideoView onInterpreterActiveLanguageChanged");
}
- (void)onJoinMeetingConfirmed{
    NSLog(@"RNZoomUsVideoView onJoinMeetingConfirmed");
}
- (void)onJBHWaitingWithCmd:(JBHCmd)cmd{
    NSLog(@"RNZoomUsVideoView onJBHWaitingWithCmd");
}
- (void)onJoinMeetingNeedUserInfo:(MobileRTCInputUserInfoHandler *)handler{
    NSLog(@"RNZoomUsVideoView onJoinMeetingNeedUserInfo");
}
- (void)onJoinMeetingInfo:(MobileRTCJoinMeetingInfo)info completion:(void (^)(NSString * _Nonnull, NSString * _Nonnull, BOOL))completion{
    NSLog(@"RNZoomUsVideoView onJoinMeetingInfo");
}
- (void)onLiveStreamStatusChange:(MobileRTCLiveStreamStatus)liveStreamStatus{
    NSLog(@"RNZoomUsVideoView onLiveStreamStatusChange");
}
- (void)onLocalVideoOrderUpdated:(NSArray<NSNumber *> *)localOrderArr{
    NSLog(@"RNZoomUsVideoView onLocalVideoOrderUpdated");
}
- (void)onLocalRecordingStatus:(NSInteger)userId status:(MobileRTCRecordingStatus)status{
    NSLog(@"RNZoomUsVideoView onLocalRecordingStatus");
}
- (void)onLiveTranscriptionMsgInfoReceived:(MobileRTCLiveTranscriptionMessageInfo *)messageInfo{
    NSLog(@"RNZoomUsVideoView onLiveTranscriptionMsgInfoReceived");
}
- (void)onLiveTranscriptionMsgError:(MobileRTCLiveTranscriptionLanguage *)speakLanguage transcriptLanguage:(MobileRTCLiveTranscriptionLanguage *)transcriptLanguage{
    NSLog(@"RNZoomUsVideoView onLiveTranscriptionMsgError");
}
- (void)onMeetingReady{
    NSLog(@"RNZoomUsVideoView onMeetingReady");
}
- (void)onMicrophoneNoPrivilege{
    NSLog(@"RNZoomUsVideoView onMicrophoneNoPrivilege");
}
- (void)onMyHandStateChange{
    NSLog(@"RNZoomUsVideoView onMyHandStateChange");
}
- (void)onMeetingLockStatus:(BOOL)isLock {
    NSLog(@"RNZoomUsVideoView onMeetingLockStatus");
}

- (void)onMyAudioStateChange {
    NSLog(@"RNZoomUsVideoView onMyAudioStateChange");
}
- (void)onMeetingEndedReason:(MobileRTCMeetingEndReason)reason{
    NSLog(@"RNZoomUsVideoView onMeetingEndedReason");
}
- (void)onMobileRTCAuthExpired{
    NSLog(@"RNZoomUsVideoView onMobileRTCAuthExpired");
}
- (void)onMicrophoneStatusError:(MobileRTCMicrophoneError)error{
    NSLog(@"RNZoomUsVideoView onMicrophoneStatusError");
}
- (void)onMobileRTCAuthReturn:(MobileRTCAuthError)returnValue{
    NSLog(@"RNZoomUsVideoView onMobileRTCAuthReturn");
}
- (void)onMeetingParameterNotification:(MobileRTCMeetingParameter *)meetingParam{
    NSLog(@"RNZoomUsVideoView onMeetingParameterNotification");
}
//- (void)onMeetingError:(MobileRTCMeetError)error message:(NSString *)message{
//    NSLog(@"RNZoomUsVideoView onMeetingError");
//}
- (void)onMobileRTCLoginResult:(MobileRTCLoginFailReason)resultValue{
    NSLog(@"RNZoomUsVideoView onMobileRTCLoginResult");
}
- (void)onMobileRTCLogoutReturn:(NSInteger)returnValue{
    NSLog(@"RNZoomUsVideoView onMobileRTCLogoutReturn");
}
- (void)onMeetingQAStatusChanged:(BOOL)isMeetingQAFeatureOn{
    NSLog(@"RNZoomUsVideoView onMeetingQAStatusChanged");
}
- (void)onNoHostMeetingWillTerminate:(NSUInteger)minutes{
    NSLog(@"RNZoomUsVideoView onNoHostMeetingWillTerminate");
}
- (void)onNotificationServiceStatus:(MobileRTCNotificationServiceStatus)status error:(MobileRTCNotificationServiceError)error{
    NSLog(@"RNZoomUsVideoView onNotificationServiceStatus");
}
- (void)onNewBroadcastMessageReceived:(NSString *)broadcastMsg senderID:(NSUInteger)senderID senderName:(NSString *)senderName {
    NSLog(@"RNZoomUsVideoView onNewBroadcastMessageReceived");
}
- (void)onOngoingShareStopped {
    NSLog(@"RNZoomUsVideoView onOngoingShareStopped");
}
- (void)onOriginalLanguageMsgReceived:(MobileRTCLiveTranscriptionMessageInfo *)messageInfo {
    NSLog(@"RNZoomUsVideoView onOriginalLanguageMsgReceived");
}
- (void)onPollingInactive {
    NSLog(@"RNZoomUsVideoView onPollingInactive");
}

- (void)onPollingListUpdated {
    NSLog(@"RNZoomUsVideoView onPollingListUpdated");
}
- (void)onPollingResultUpdated:(NSString *)pollingID{
    NSLog(@"RNZoomUsVideoView onPollingResultUpdated");
}
- (void)onPollingStatusChanged:(NSString *)pollingID status:(MobileRTCPollingStatus)status {
    NSLog(@"RNZoomUsVideoView onPollingStatusChanged");
}
- (void)onProxyAuth:(NSString *)host port:(NSUInteger)port completion:(void (^)(NSString * _Nonnull, NSUInteger, NSString * _Nonnull, NSString * _Nonnull, BOOL))completion {
    NSLog(@"RNZoomUsVideoView onProxyAuth");
}
- (void)onParticipantProfilePictureStatusChange:(BOOL)hidden {
    NSLog(@"RNZoomUsVideoView onParticipantProfilePictureStatusChange");
}
- (void)onPollingElapsedTime:(NSString *)pollingID uElapsedtime:(int)uElapsedtime {
    NSLog(@"RNZoomUsVideoView onPollingElapsedTime");
}
- (void)onPollingQuestionImageDownloaded:(NSString *)questionID path:(NSString *)path {
    NSLog(@"RNZoomUsVideoView onPollingQuestionImageDownloaded");
}
- (void)onPollingActionResult:(MobileRTCPollingActionType)actionType pollingID:(NSString *)pollingID bSuccess:(BOOL)bSuccess errorMsg:(NSString *)errorMsg {
    NSLog(@"RNZoomUsVideoView onPollingActionResult");
}
- (void)onRecordingStatus:(MobileRTCRecordingStatus)status {
    NSLog(@"RNZoomUsVideoView onRecordingStatus");
}
- (void)onRequestSignInterpreterToTalk {
    NSLog(@"RNZoomUsVideoView onRequestSignInterpreterToTalk");
}
- (void)onRequestCloudRecordingResponse:(MobileRTCRequestStartCloudRecordingStatus)status {
    NSLog(@"RNZoomUsVideoView onRequestCloudRecordingResponse");
}
- (void)onRawLiveStreamPrivilegeChanged:(BOOL)hasPrivilege {
    NSLog(@"RNZoomUsVideoView onRawLiveStreamPrivilegeChanged");
}
- (void)onRawLiveStreamPrivilegeRequested:(MobileRTCRequestRawLiveStreamPrivilegeHandler *)handler {
    NSLog(@"RNZoomUsVideoView onRawLiveStreamPrivilegeRequested");
}
- (void)onRawLiveStreamPrivilegeRequestTimeout {
    NSLog(@"RNZoomUsVideoView onRawLiveStreamPrivilegeRequestTimeout");
}
- (void)onRequestLocalRecordingPrivilegeChanged:(MobileRTCLocalRecordingRequestPrivilegeStatus)status {
    NSLog(@"RNZoomUsVideoView onRequestLocalRecordingPrivilegeChanged");
}
- (void)onRequestLocalRecordingPrivilegeReceived:(MobileRTCRequestLocalRecordingPrivilegeHandler *)handler {
    NSLog(@"RNZoomUsVideoView onRequestLocalRecordingPrivilegeReceived");
}
- (void)onSinkQAConnected:(BOOL)connected{
    NSLog(@"RNZoomUsVideoView onSinkQAConnected");
}
- (void)onSinkDeleteAnswer:(NSArray<NSString *> *)answerIDArray{
    NSLog(@"RNZoomUsVideoView onSinkDeleteAnswer");
}
- (void)onSinkLowerAllHands{
    NSLog(@"RNZoomUsVideoView onSinkLowerAllHands");
}
- (void)onSuspendParticipantsActivities{
    NSLog(@"RNZoomUsVideoView onSuspendParticipantsActivities");
}
- (void)onSinkReceiveAnswer:(NSString *)answerID{
    NSLog(@"RNZoomUsVideoView onSinkReceiveAnswer");
}
- (void)onSinkDeleteQuestion:(NSArray<NSString *> *)questionIDArray{
    NSLog(@"RNZoomUsVideoView onSinkDeleteQuestion");
}
- (void)onSinkReopenQuestion:(NSString *)questionID{
    NSLog(@"RNZoomUsVideoView onSinkReopenQuestion");
}
- (void)onSinkQAConnectStarted{
    NSLog(@"RNZoomUsVideoView onSinkQAConnectStarted");
}
- (void)onSinkReceiveQuestion:(NSString *)questionID {
    NSLog(@"RNZoomUsVideoView onSinkReceiveQuestion");
}
- (void)onSinkUserEndLiving:(NSString *)questionID {
    NSLog(@"RNZoomUsVideoView onSinkUserEndLiving");
}
- (void)onSignInterpreterListChanged {
    NSLog(@"RNZoomUsVideoView onSignInterpreterListChanged");
}
- (void)onSignInterpreterRoleChanged {
    NSLog(@"RNZoomUsVideoView onSignInterpreterRoleChanged");
}
- (void)onSinkPanelistCapacityExceed {
    NSLog(@"RNZoomUsVideoView onSinkPanelistCapacityExceed");
}
- (void)onSinkUserLivingReply:(NSString *)questionID {
    NSLog(@"RNZoomUsVideoView onSinkUserLivingReply");
}
- (void)onSignInterpreterLanguageChanged {
    NSLog(@"RNZoomUsVideoView onSignInterpreterLanguageChanged");
}
- (void)onSinkWebinarNeedRegister:(NSString *)registerURL{
    NSLog(@"RNZoomUsVideoView onSinkWebinarNeedRegister");
}
- (void)onSmartSummaryStatusChange:(BOOL)isStarted {
    NSLog(@"RNZoomUsVideoView onSmartSummaryStatusChange");
}
- (void)onSpotlightVideoUserChange:(NSArray<NSNumber *> *)spotlightedUserList{
    NSLog(@"RNZoomUsVideoView onSpotlightVideoUserChange");
}
- (void)onSinkLiveTranscriptionStatus:(MobileRTCLiveTranscriptionStatus)status{
    NSLog(@"RNZoomUsVideoView onSinkLiveTranscriptionStatus");
}
- (void)onSinkSelfAllowTalkNotification{
    NSLog(@"RNZoomUsVideoView onSinkSelfAllowTalkNotification");
}
- (void)onStartCloudRecordingRequested:(MobileRTCRequestStartCloudRecordingPrivilegeHandler *)handler{
    NSLog(@"RNZoomUsVideoView onStartCloudRecordingRequested");
}
- (void)onSignInterpretationStatusChange:(MobileRTCSignInterpretationStatus)status{
    NSLog(@"RNZoomUsVideoView onSignInterpretationStatusChange");
}
- (void)onSinkSelfDisallowTalkNotification{
    NSLog(@"RNZoomUsVideoView onSinkSelfDisallowTalkNotification");
}
- (void)onSinkQAOpenQuestionChanged:(NSInteger)count{
    NSLog(@"RNZoomUsVideoView onSinkQAOpenQuestionChanged");
}
- (void)onSinkMeetingAudioTypeChange:(NSUInteger)userID{
    NSLog(@"RNZoomUsVideoView onSinkMeetingAudioTypeChange");
}
- (void)onSinkMeetingMyAudioTypeChange{
    NSLog(@"RNZoomUsVideoView onSinkMeetingMyAudioTypeChange");
}
- (void)onSinkShareSettingTypeChanged:(MobileRTCShareSettingType)shareSettingType{
    NSLog(@"RNZoomUsVideoView onSinkShareSettingTypeChanged");
}
- (void)onSinkQuestionMarkedAsDismissed:(NSString *)questionID{
    NSLog(@"RNZoomUsVideoView onSinkQuestionMarkedAsDismissed");
}
- (void)onSinkQAAddAnswer:(NSString *)answerID success:(BOOL)success{
    NSLog(@"RNZoomUsVideoView onSinkQAAddAnswer");
}
- (void)onSinkJoin3rdPartyTelephonyAudio:(NSString *)audioInfo{
    NSLog(@"RNZoomUsVideoView onSinkJoin3rdPartyTelephonyAudio");
}

- (void)onSinkPanelistChatPrivilegeChanged:(MobileRTCPanelistChatPrivilegeType)privilege{
    NSLog(@"RNZoomUsVideoView onSinkPanelistChatPrivilegeChanged");
}
- (void)onSinkQAAddQuestion:(NSString *)questionID success:(BOOL)success{
    NSLog(@"RNZoomUsVideoView onSinkQAAddQuestion");
}
- (void)onSinkAllowAttendeeChatNotification:(MobileRTCChatAllowAttendeeChat)currentPrivilege{
    NSLog(@"RNZoomUsVideoView onSinkAllowAttendeeChatNotification");
}
- (void)onSinkAttendeeChatPriviledgeChanged:(MobileRTCMeetingChatPriviledgeType)currentPrivilege{
    NSLog(@"RNZoomUsVideoView onSinkAttendeeChatPriviledgeChanged");
}
- (void)onSinkPromptAttendee2PanelistResult:(MobileRTCWebinarPromoteorDepromoteError)errorCode{
    NSLog(@"RNZoomUsVideoView onSinkPromptAttendee2PanelistResult");
}
- (void)onSinkVoteupQuestion:(NSString *)questionID orderChanged:(BOOL)orderChanged{
    NSLog(@"RNZoomUsVideoView onSinkVoteupQuestion");
}
- (void)onSinkDePromptPanelist2AttendeeResult:(MobileRTCWebinarPromoteorDepromoteError)errorCode{
    NSLog(@"RNZoomUsVideoView onSinkDePromptPanelist2AttendeeResult");
}
- (void)onSmartSummaryPrivilegeRequested:(NSInteger)userId handler:(MobileRTCSmartSummaryPrivilegeHandler *)handler{
    NSLog(@"RNZoomUsVideoView onSmartSummaryPrivilegeRequested");
}
- (void)onSubscribeUserFail:(MobileRTCSubscribeFailReason)errorCode size:(NSInteger)size userId:(NSUInteger)userId {
    NSLog(@"RNZoomUsVideoView onSubscribeUserFail");
}
- (void)onSmartSummaryStartReqResponse:(BOOL)timeout decline:(BOOL)isDecline{
    NSLog(@"RNZoomUsVideoView onSmartSummaryStartReqResponse");
}
- (void)onSinkRevokeVoteupQuestion:(NSString *)questionID orderChanged:(BOOL)orderChanged{
    NSLog(@"RNZoomUsVideoView onSinkRevokeVoteupQuestion");
}
- (void)onSinkQAAllowAskQuestionAnonymouslyNotification:(BOOL)beAllowed{
    NSLog(@"RNZoomUsVideoView onSinkQAAllowAskQuestionAnonymouslyNotification");
}
- (void)onSinkQAAllowAttendeeAnswerQuestionNotification:(BOOL)beAllowed{
    NSLog(@"RNZoomUsVideoView onSinkQAAllowAttendeeAnswerQuestionNotification");
}
- (void)onSinkMeetingVideoQualityChanged:(MobileRTCVideoQuality)qality userID:(NSUInteger)userID{
    NSLog(@"RNZoomUsVideoView onSinkMeetingVideoQualityChanged");
}
- (void)onSinkAttendeePromoteConfirmResult:(BOOL)agree userId:(NSUInteger)userId{
    NSLog(@"RNZoomUsVideoView onSinkAttendeePromoteConfirmResult");
}
- (void)onSendPairingCodeStateChanged:(MobileRTCH323ParingStatus)state MeetingNumber:(unsigned long long)meetingNumber{
    NSLog(@"RNZoomUsVideoView onSendPairingCodeStateChanged");
}
- (void)onSinkQAAllowAttendeeUpVoteQuestionNotification:(BOOL)beAllowed{
    NSLog(@"RNZoomUsVideoView onSinkQAAllowAttendeeUpVoteQuestionNotification");
}
- (void)onSinkQAAllowAttendeeViewAllQuestionNotification:(BOOL)beAllowed{
    NSLog(@"RNZoomUsVideoView onSinkQAAllowAttendeeViewAllQuestionNotification");
}
- (void)onSinkMeetingShowMinimizeMeetingOrBackZoomUI:(MobileRTCMinimizeMeetingState)state{
    NSLog(@"RNZoomUsVideoView onSinkMeetingShowMinimizeMeetingOrBackZoomUI");
}

- (void)onSinkRequestForLiveTranscriptReceived:(NSUInteger)requesterUserId bAnonymous:(BOOL)bAnonymous{
    NSLog(@"RNZoomUsVideoView onSinkRequestForLiveTranscriptReceived");
}
- (void)onShareFromMainSession:(NSUInteger)sharingID shareStatus:(MobileRTCSharingStatus)status shareAction:(MobileRTCShareAction *)shareAction{
    NSLog(@"RNZoomUsVideoView onShareFromMainSession");
}
- (void)onSinkLiveTranscriptionMsgReceived:(NSString *)msg speakerId:(NSUInteger)speakerId type:(MobileRTCLiveTranscriptionOperationType)type{
    NSLog(@"RNZoomUsVideoView onSinkLiveTranscriptionMsgReceived");
}
- (void)onSinkJoinWebinarNeedUserNameAndEmailWithCompletion:(BOOL (^)(NSString * _Nonnull, NSString * _Nonnull, BOOL))completion{
    NSLog(@"RNZoomUsVideoView onSinkJoinWebinarNeedUserNameAndEmailWithCompletion");
}
- (void)onTalkPrivilegeChanged:(BOOL)hasPrivilege{
    NSLog(@"RNZoomUsVideoView onTalkPrivilegeChanged");
}

- (void)onUpgradeFreeMeetingResult:(NSUInteger)result{
    NSLog(@"RNZoomUsVideoView onUpgradeFreeMeetingResult");
}
- (void)onUserConfirmToStartArchive:(MobileRTCArchiveConfrimHandle *)handler{
    NSLog(@"RNZoomUsVideoView onUserConfirmToStartArchive");
}
- (void)onUserRawLiveStreamingStatusChanged:(NSArray<MobileRTCRawLiveStreamInfo *> *)liveStreamList{
    NSLog(@"RNZoomUsVideoView onUserRawLiveStreamingStatusChanged");
}
- (void)onUserRawLiveStreamPrivilegeChanged:(NSUInteger)userId hasPrivilege:(_Bool)hasPrivilege{
    NSLog(@"RNZoomUsVideoView onUserRawLiveStreamPrivilegeChanged");
}
- (void)onVideoAlphaChannelStatusChanged:(BOOL)alphaChannelOn{
    NSLog(@"RNZoomUsVideoView onVideoAlphaChannelStatusChanged");
}
- (void)onWhiteboardStatusChanged:(MobileRTCWhiteboardStatus)status{
    NSLog(@"RNZoomUsVideoView onWhiteboardStatusChanged");
}
- (void)onWaitingRoomUserJoin:(NSUInteger)userId{
    NSLog(@"RNZoomUsVideoView onWaitingRoomUserJoin");
}
- (void)onWaitingRoomUserLeft:(NSUInteger)userId{
    NSLog(@"RNZoomUsVideoView onWaitingRoomUserLeft");
}
- (void)onWaitingRoomStatusChange:(BOOL)needWaiting{
    NSLog(@"RNZoomUsVideoView onWaitingRoomStatusChange");
}
- (void)onWaitingRoomEntranceEnabled:(BOOL)enabled{
    NSLog(@"RNZoomUsVideoView onWaitingRoomEntranceEnabled");
}
- (void)onWebinarNeedInputScreenName:(MobileRTCWebinarInputScreenNameHandler *)handler{
    NSLog(@"RNZoomUsVideoView onWebinarNeedInputScreenName");
}
- (void)onWaitingRoomPresetAudioStatusChanged:(BOOL)audioCanTurnOn{
    NSLog(@"RNZoomUsVideoView onWaitingRoomPresetAudioStatusChanged");
}
- (void)onWaitingRoomPresetVideoStatusChanged:(BOOL)videoCanTurnOn{
    NSLog(@"RNZoomUsVideoView onWaitingRoomPresetVideoStatusChanged");
}
- (void)onWaitingRoomUserNameChanged:(NSInteger)userID userName:(NSString *)userName{
    NSLog(@"RNZoomUsVideoView onWaitingRoomUserNameChanged");
}
- (void)onWhiteboardSettingsChanged:(MobileRTCWhiteboardShareOption)shareOption createOption:(MobileRTCWhiteboardCreateOption)createOption enable:(BOOL)enable{
    NSLog(@"RNZoomUsVideoView onWhiteboardSettingsChanged");
}
- (void)onZoomIdentityExpired{
    NSLog(@"RNZoomUsVideoView onZoomIdentityExpired");
}

- (void)setMuteMyAudio:(BOOL *)isMute{
    if (_rnZoomUsVideoViewController)
    {
        //        [_rnZoomUsVideoViewController setMuteMyAudio:isMute == Nil ? NO : YES];
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
        [ms muteMyAudio:isMute == Nil ? NO : YES];
    }
}

- (void) countInMeetingUser {
    MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
    NSUInteger inMeetingUserCount = [[ms getInMeetingUserList] count];
    NSLog(@"RNZoomUsVideoView onInMeetingUserUpdated==%@", @(inMeetingUserCount));
    if (self.onInMeetingUserCount) {
        self.onInMeetingUserCount(@{
            @"event": @"meetingCount",
            @"userList": @(inMeetingUserCount)
        });
    }
}

- (void)setMuteMyCamera:(BOOL *)isMute{
    if (_rnZoomUsVideoViewController)
    {
        //        [_rnZoomUsVideoViewController setMuteMyCamera:isMute == Nil ? NO : YES];
        MobileRTCMeetingService *ms = [[MobileRTC sharedRTC] getMeetingService];
        [ms muteMyVideo:isMute == Nil ? NO : YES];
    }
}

- (void)setFullScreen:(BOOL *)fullScreen {
    NSLog(@"RNZoomUsVideoView setFullScreen");
    [[GlobalData sharedInstance] setGlobalOrientation:fullScreen == Nil ? 1 : 3];
}


//- (BOOL)onCheckIfMeetingVoIPCallRunning{
//    return [providerDelegate isInCall];
//}
@end
