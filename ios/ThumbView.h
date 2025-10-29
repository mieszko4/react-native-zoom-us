//
//  ThumbView.h
//  MobileRTCSample
//
//  Created by Zoom Video Communications on 2018/10/15.
//  Copyright © 2018 Zoom Video Communications, Inc. All rights reserved.
//

#import <UIKit/UIKit.h>

#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)
#define Bottom_Height           (IPHONE_X ? (60 + 34) : 60)
#define IPHONE_X \
({BOOL isPhoneX = NO;\
if (@available(iOS 11.0, *)) {\
isPhoneX = [[UIApplication sharedApplication] delegate].window.safeAreaInsets.bottom > 0.0;\
}\
(isPhoneX);})
#define SAFE_ZOOM_INSETS  34

const static CGFloat BTN_HEIGHT = 24;

@interface ThumbView : UIView
@property (nonatomic)         NSUInteger                  pinUserID;
@property (nonatomic,copy) void(^pinOnClickBlock)(NSInteger pinUserID);
- (void)updateFrame;
- (void)updateThumbViewVideo;
- (void)showThumbView;
- (void)hiddenThumbView;
@end

