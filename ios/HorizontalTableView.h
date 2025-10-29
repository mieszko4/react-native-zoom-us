//
//  HorizonTableView.h
//  MobileRTCSample
//
//  Created by Zoom Video Communications on 14/11/2017.
//  Copyright © 2017 Zoom Video Communications, Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)

@interface HorizontalTableView : UITableView

@property (assign, nonatomic) BOOL isDragging;

@end
