//
//  VideoViewController.m
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/11.
//
//

#import "VideoViewController.h"
#import <MobileRTC/MobileRTC.h>
#import "GlobalData.h"

@interface VideoViewController ()

@end

@implementation VideoViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    [self.view addSubview:self.activeVideoView];
    [self.view addSubview:self.videoView];
    self.activeVideoView.hidden = YES;
    self.videoView.hidden = YES;
    
    [self.view addSubview:self.preVideoView];
    self.preVideoView.hidden = YES;
    
    self.lastUserID = NSNotFound;
    self.lastOrientation = UIInterfaceOrientationUnknown;
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
}

- (void)viewDidDisappear:(BOOL)animated
{
    [super viewDidDisappear:animated];
}

- (void)dealloc
{
    self.videoView = nil;
    self.preVideoView = nil;
    self.activeVideoView = nil;
    self.lastUserID = NSNotFound;
    self.lastOrientation = UIInterfaceOrientationUnknown;
}


- (void)showAttendeeVideoWithUserID:(NSUInteger)userID
{
    self.activeVideoView.hidden = YES;
    if (!self.videoView.superview) {
        [self.view addSubview:self.videoView];
    }
    self.videoView.hidden = NO;
    [self.view bringSubviewToFront:self.videoView];
    [self.videoView showAttendeeVideoWithUserID:userID];
    
    CGRect frame = self.view.frame;
    frame.origin.y = 0;
    self.view.frame = frame;
    CGRect videoView = self.videoView.frame;
    videoView.origin.y = 0;
    self.videoView.frame = videoView;
}

- (void)showActiveVideoWithUserID:(NSUInteger)userID
{
    UIInterfaceOrientation currentOrientation = [[GlobalData sharedInstance] globalOrientation];
    
    if (userID == self.lastUserID && currentOrientation == self.lastOrientation) {
        return;
    }

    self.lastUserID = userID;
    self.lastOrientation = currentOrientation;
    
    self.videoView.hidden = YES;
    
    [self.activeVideoView stopAttendeeVideo];
    
    if (!self.activeVideoView.superview) {
        [self.view addSubview:self.activeVideoView];
    }
    self.activeVideoView.hidden = NO;
    [self.view bringSubviewToFront:self.activeVideoView];
    [self.activeVideoView showAttendeeVideoWithUserID:userID];
    CGRect frame = self.view.frame;
    frame.origin.y = 0;
    self.view.frame = frame;
    CGRect activeVideoViewFrame = self.activeVideoView.frame;
    activeVideoViewFrame.origin.y = 0;
    self.activeVideoView.frame = activeVideoViewFrame;
}


- (void)stopActiveVideo {
    [self.activeVideoView stopAttendeeVideo];
}

- (void)viewDidLayoutSubviews
{
    [super viewDidLayoutSubviews];
    CGRect frame = self.view.bounds;
    self.videoView.frame = frame;
}

- (MobileRTCVideoView*)videoView
{
    if (!_videoView)
    {
        _videoView = [[MobileRTCVideoView alloc] initWithFrame:self.view.bounds];
        [_videoView setVideoAspect:MobileRTCVideoAspect_PanAndScan];
    }
    return _videoView;
}

- (MobileRTCActiveVideoView *)activeVideoView
{
//    UIWindowScene *windowScene = (UIWindowScene *)[UIApplication.sharedApplication.connectedScenes anyObject];
//    UIInterfaceOrientation orientation = windowScene.interfaceOrientation;
//    NSLog(@"VideoViewController activeVideoView orientation: %ld", (long)orientation);
//    self.lastOrientation = orientation;
    
    BOOL isLandscape = UIInterfaceOrientationIsLandscape(self.lastOrientation);
    
    CGFloat defaultWidth = self.view.bounds.size.width;
    CGFloat videoAspectRatio = 1.77777777778;
    CGFloat desiredHeight = isLandscape ? self.view.bounds.size.height : defaultWidth/videoAspectRatio;
    if (!_activeVideoView)
    {
        CGRect frame = CGRectMake(0, 0, self.view.bounds.size.width, desiredHeight);
        _activeVideoView = [[MobileRTCActiveVideoView alloc] initWithFrame:frame];
        [_videoView setVideoAspect:MobileRTCVideoAspect_PanAndScan];
    }
        CGRect currentFrame = CGRectMake(0, 0, self.view.bounds.size.width, desiredHeight);
        _activeVideoView.frame =currentFrame;
    return _activeVideoView;
}

- (MobileRTCPreviewVideoView*)preVideoView
{
    if (!_preVideoView)
    {
        _preVideoView = [[MobileRTCPreviewVideoView alloc] initWithFrame:self.view.bounds];
        [_preVideoView setVideoAspect:MobileRTCVideoAspect_PanAndScan];
    }
    return _preVideoView;
}

/*
 #pragma mark - Navigation
 
 // In a storyboard-based application, you will often want to do a little preparation before navigation
 - (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
 // Get the new view controller using [segue destinationViewController].
 // Pass the selected object to the new view controller.
 }
 */

@end
