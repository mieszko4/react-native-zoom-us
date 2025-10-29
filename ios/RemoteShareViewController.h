//
//  RemoteShareViewController.h
//  react-native-zoom-us
//
//  Created by John Vu on 2024/05/12.
//  
//

#import <UIKit/UIKit.h>
#import <MobileRTC/MobileRTC.h>

@interface RemoteShareViewController : UIViewController

//@property (assign, nonatomic) NSUInteger activeShareID;
@property (nonatomic, strong) MobileRTCShareAction *shareAction;

@property (strong, nonatomic) MobileRTCActiveShareView* shareView;

- (void)updateShareView;
- (void)stopShareView;

@end
