
#import "RNZoomUs.h"

@implementation RNZoomUs
{
  RCTPromiseResolveBlock initializePromiseResolve;
  RCTPromiseRejectBlock initializePromiseReject;
  BOOL isInitialized;
}

- (instancetype)init {
  if (self = [super init]) {
    isInitialized = NO;
    initializePromiseResolve = nil;
    initializePromiseReject = nil;
  }
  return self;
}

+ requiresMainQueueSetup
{
  return NO;
}

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

RCT_EXPORT_MODULE(
  NSString *appKey,
  NSString *appSecret,
  NSString *webDomain,
  RCTPromiseResolveBlock resolve,
  RCTPromiseRejectBlock reject
)
{
  if (isInitialized) {
    resolve(@"Already initialize Zoom SDK successfully.");
    return;
  }

  isInitialized = true;

  // TODO: try catch with reject("ERR_UNEXPECTED_EXCEPTION", ex);
  // TODO: zoomsdk init code
  initializePromiseResolve = resolve;
  initializePromiseReject = reject;

  resolve(@"DOING IT!");
}
@end
