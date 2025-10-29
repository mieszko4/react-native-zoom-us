//
//  ThumbViewController.m
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/12.
//
//

#import "ThumbViewController.h"
#import <MobileRTC/MobileRTC.h>

@interface ThumbViewController ()

@end

@implementation ThumbViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    [self.view addSubview:self.thumbView];
//    self.thumbView.hidden = YES;
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
    self.thumbView = nil;
}


- (void)showAttendeeVideoWithUserID:(NSUInteger)userID
{
    if (!self.thumbView.superview) {
        [self.view addSubview:self.thumbView];
    }
    self.thumbView.hidden = NO;
    [self.view bringSubviewToFront:self.thumbView];
    [self.thumbView showAttendeeVideoWithUserID:userID];
}

- (void)stopActiveVideo {
    [self.thumbView stopAttendeeVideo];
}

- (void)viewDidLayoutSubviews
{
    [super viewDidLayoutSubviews];
    CGRect frame = self.view.bounds;
    self.thumbView.frame = frame;
}

- (MobileRTCPreviewVideoView*)thumbView
{
    if (!_thumbView)
    {
        _thumbView = [[MobileRTCPreviewVideoView alloc] initWithFrame:CGRectMake(0, 0, 100, 100)];
        _thumbView.layer.cornerRadius = 10;
        _thumbView.layer.borderWidth = 1;
    }
    return _thumbView;
}
@end
