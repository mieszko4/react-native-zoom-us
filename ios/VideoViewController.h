//
//  VideoViewController.h
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/11.
//  
//

#import <UIKit/UIKit.h>
#import <MobileRTC/MobileRTC.h>
@interface VideoViewController : UIViewController
@property (strong, nonatomic) MobileRTCPreviewVideoView  * preVideoView;
@property (strong, nonatomic) MobileRTCVideoView         * videoView;
@property (strong, nonatomic) MobileRTCActiveVideoView   * activeVideoView;
@property (nonatomic, assign) NSUInteger lastUserID;
@property (nonatomic, assign) UIInterfaceOrientation lastOrientation;

- (void)showAttendeeVideoWithUserID:(NSUInteger)userID;
- (void)showActiveVideoWithUserID:(NSUInteger)userID;
- (void)stopActiveVideo;

@end

