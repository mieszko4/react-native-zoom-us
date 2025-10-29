//
//  ThumbPreviewView.m
//  RNZoomUs
//
//  Created by Đức Vũ on 28/4/25.
//

#import <Foundation/Foundation.h>
#import "ThumbPreviewView.h"
#import <MobileRTC/MobileRTC.h>

@interface ThumbPreviewView ()
@property (strong, nonatomic) MobileRTCVideoView *videoView;
@property (strong, nonatomic) MobileRTCVideoView *preVideoView;
@end

@implementation ThumbPreviewView

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:CGRectZero];
    if (self) {
        [self updateLayout];
        [self addSubview:self.preVideoView];
    }
    return self;
}

- (MobileRTCVideoView *)videoView {
    if (!_videoView) {
        _videoView = [[MobileRTCVideoView alloc] initWithFrame:self.bounds];
        _videoView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        _videoView.backgroundColor = [UIColor blackColor];
    }
    return _videoView;
}

- (MobileRTCPreviewVideoView *)preVideoView {
    if (!_preVideoView) {
        _preVideoView = [[MobileRTCPreviewVideoView alloc] initWithFrame:self.bounds];
        [_preVideoView setVideoAspect:MobileRTCVideoAspect_PanAndScan];
        _preVideoView.backgroundColor = [UIColor blackColor];
        _preVideoView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    }
    return _preVideoView;
}

- (void)startPreviewWithMyself {
    MobileRTCMeetingService *meetingService = [[MobileRTC sharedRTC] getMeetingService];
    NSUInteger myUserID = [meetingService myselfUserID];
    if (myUserID != 0) {
        [self.videoView showAttendeeVideoWithUserID:myUserID];
        [self.videoView setVideoAspect:MobileRTCVideoAspect_PanAndScan];
    }
}

- (void)stopPreview {
    [self.videoView stopAttendeeVideo];
}
- (void)updateLayout {
    UIWindowScene *windowScene = (UIWindowScene *)[UIApplication.sharedApplication.connectedScenes anyObject];
    UIInterfaceOrientation orientation = windowScene.interfaceOrientation;
    BOOL isLandscape = UIInterfaceOrientationIsLandscape(orientation);
    
    CGFloat screenWidth = UIScreen.mainScreen.bounds.size.width;
    CGFloat screenHeight = UIScreen.mainScreen.bounds.size.height;
    CGFloat videoAspectRatio = 1.77777777778;
    
    CGFloat previewWidth = isLandscape ? screenHeight / 7.0 : screenWidth / 7.0;
    CGFloat previewHeight = previewWidth * videoAspectRatio;
    
    CGFloat x = screenWidth - previewWidth - 5;
    CGFloat y = 5;
    
    self.frame = CGRectMake(x, y, previewWidth, previewHeight);
    self.videoView.frame = self.bounds;
}
@end
