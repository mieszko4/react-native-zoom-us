//
//  ProviderDelegate.m
//  MobileRTCSample
//
//  Created by John Vu on 29/6/24.
//  Copyright © 2024 Zoom Video Communications, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "ProviderDelegate.h"

@implementation ProviderDelegate
- (instancetype)init {
    self = [super init];
    if (self) {
        self.provider = [[CXProvider alloc] initWithConfiguration:[ProviderDelegate providerConfiguration]];
        [self.provider setDelegate:self queue:nil];
    }
    return self;
}

- (void)providerDidReset:(CXProvider *)provider {
    self.callingUUID = nil;
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action {
    [action fulfill];
}

- (BOOL)isInCall {
    return self.callingUUID != nil;
}

+ (CXProviderConfiguration *)providerConfiguration {
    CXProviderConfiguration *providerConfiguration = [[CXProviderConfiguration alloc] initWithLocalizedName:@"Minh Trí Thành"];
    providerConfiguration.supportedHandleTypes = [NSSet setWithObject:@(CXHandleTypeGeneric)];
    providerConfiguration.supportsVideo = YES;
    
    return providerConfiguration;
}

@end
