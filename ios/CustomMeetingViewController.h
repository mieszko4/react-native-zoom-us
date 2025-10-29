//
//  CustomMeetingViewController.h
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/12.
//  
//

#import <UIKit/UIKit.h>
#import "VideoViewController.h"
#import "RemoteShareViewController.h"
#import "ThumbViewController.h"
#import "ThumbView.h"
#import "ThumbPreviewView.h"

@interface CustomMeetingViewController : UIViewController <MobileRTCMeetingServiceDelegate>

@property (strong, nonatomic) UIView                    * baseView;
//@property (assign, nonatomic) NSInteger                 pinUserId;
@property (strong, nonatomic) ThumbView                 * thumbView;
@property (strong, nonatomic) ThumbPreviewView          * thumbPreviewView;

@property (strong, nonatomic) NSMutableArray                * vcArray;
@property (strong, nonatomic) VideoViewController           * videoVC;
@property (strong, nonatomic) RemoteShareViewController     * remoteShareVC;

@property (assign, nonatomic) BOOL          muteMyAudio;
@property (assign, nonatomic) BOOL          muteMyCamera;

- (void)updateVideoOrShare;
- (void)updateMyAudioStatus;
- (void)updateMyVideoStatus;

- (void)showVideoView;
- (void)showRemoteShareView;

@end

