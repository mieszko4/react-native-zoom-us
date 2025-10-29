//
//  ProviderDelegate.h
//  MobileRTCSample
//
//  Created by John Vu on 29/6/24.
//  Copyright © 2024 Zoom Video Communications, Inc. All rights reserved.
//

#ifndef ProviderDelegate_h
#define ProviderDelegate_h


#endif /* ProviderDelegate_h */
#import <CallKit/CallKit.h>
@interface ProviderDelegate : NSObject <CXProviderDelegate>
@property (nonatomic, strong) CXProvider *provider;
@property (nonatomic, strong) NSUUID *callingUUID;
- (BOOL)isInCall;
@end
