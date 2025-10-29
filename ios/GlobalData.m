#import "GlobalData.h"

@implementation GlobalData

+ (instancetype)sharedInstance {
    static GlobalData *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

@end