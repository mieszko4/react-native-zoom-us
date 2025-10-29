//
//  ThumbViewController.h
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/12.
//
//

#import <UIKit/UIKit.h>
#import <MobileRTC/MobileRTC.h>
@interface ThumbViewController : UIViewController
@property (strong, nonatomic) MobileRTCPreviewVideoView         * thumbView;

- (void)showAttendeeVideoWithUserID:(NSUInteger)userID;
- (void)stopActiveVideo;

@end

