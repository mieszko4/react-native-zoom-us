## Changelog

### 6.7.0
Android updates:
- Updated ZoomSDK to 5.9.1.3674
- Added new installation steps
- Updated native dependencies
- Added new required listeners
  - onSharingStatus(SharingStatus status, long userId)
  - onFollowHostVideoOrderChanged(boolean bFollow)
  - onVideoOrderUpdated(List<Long> orderList)
- Updated renamed listener
  - onMeetingNeedCloseOtherMeeting(InMeetingEventHandler handler)

iOS updates:
- Updated ZoomSDK to 5.9.1.2191
- Added new required listeners
  - onSinkSharingStatus:(MobileRTCSharingStatus)status userID:(NSUInteger)userID
  - onVideoOrderUpdated:(NSArray <NSNumber *>* _Nullable)orderArr
  - onFollowHostVideoOrderChanged:(BOOL)follow

Be aware that you might need to follow the android installation steps to get this working on android 28 and above.

### 6.4.0

Android updates:
- Fix share screen for Custom View
### 6.3.1

- Upgrade iOS ZoomSDK to 5.7.1.645
- Upgrade android ZoomSDK to 5.7.1.1268

### 6.1.0

iOS updates:
- Upgrade ios ZoomSDk to 5.7.1.644
- Add `isInitialized()` method
### 6.0.0

Android updates:
- Upgrade android ZoomSDK to 5.7.1.1267
- Delete local archive repository for .arr files
- `participant_id` param is removed from sdk
- `Breaking changes`: Make sure to check [upgrading v6.0.0](docs/UPGRADING.md#600-jitpack)

### 5.12.0

- (ios) Upgrade ZoomSDK to 5.5.12511.0421
