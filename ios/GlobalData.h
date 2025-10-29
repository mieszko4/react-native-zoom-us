@interface GlobalData : NSObject

+ (instancetype)sharedInstance;
@property (nonatomic, assign) NSUInteger userID;
@property (nonatomic, assign) NSUInteger globalActiveShareID;
@property (nonatomic, assign) NSUUID *globalCallingUUID;
@property (nonatomic, assign) NSUInteger globalWebinarFirstActiveVideoID;
@property (nonatomic, assign) BOOL globalIsInMeeting;
@property (nonatomic, assign) UIInterfaceOrientation globalOrientation;
@property (nonatomic, assign) BOOL globalIsConnectedDefaultAudioSession;

@end
