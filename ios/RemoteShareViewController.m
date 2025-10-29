//
//  RemoteShareViewController.m
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/12.
//
//

#import "RemoteShareViewController.h"
#import "GlobalData.h"

@interface RemoteShareViewController ()


@end

@implementation RemoteShareViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    self.view.backgroundColor = [UIColor clearColor];

    [self.view addSubview:self.shareView];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)dealloc
{
    self.shareView = nil;
}

- (void)viewDidLayoutSubviews
{
    [super viewDidLayoutSubviews];
    
    self.shareView.frame = self.view.bounds;
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    
    [self updateShareView];
}

- (void)viewDidDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    
    [self.shareView stopActiveShare];
}

#pragma mark - MobileRTCVideoView

- (MobileRTCActiveShareView *)shareView
{
    if (!_shareView)
    {
        _shareView = [[MobileRTCActiveShareView alloc] initWithFrame:self.view.bounds];
    }
    return _shareView;
}

- (void)updateShareView
{
    NSUInteger globalActiveShareID = [[GlobalData sharedInstance] globalActiveShareID];
    if (0 != globalActiveShareID)
    {
        [self.shareView showActiveShareWithShareSourceID:globalActiveShareID];
        MobileRTCAnnotationService *as = [[MobileRTC sharedRTC] getAnnotationService];
        if (as) [as startAnnotationWithSharedView:self.shareView];
    }
    else
    {
        [self.shareView stopActiveShare];
    }
}


- (void)stopShareView {
    [self.shareView stopActiveShare];
}
@end
