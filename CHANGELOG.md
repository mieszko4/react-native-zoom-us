## Changelog

### 10.0.0

iOS:

- Updated ZoomSDK to 6.1.0.16235

### 9.0.0

Android:

- Updated ZoomSDK to 6.1.5.23231

### 8.0.0

Android:

- Updated ZoomSDK to 5.17.11.20433

### 7.0.0

iOS:

- Updated ZoomSDK to 5.17.11.14222

### 6.20.0

iOS:

- Updated ZoomSDK to 5.14.11.8690
- BREAKING CHANGE: removed clientKey/clientSecret for initialize - use jwtToken instead

### 6.19.0

Android:

- Updated ZoomSDK to 5.16.2.16555
- BREAKING CHANGE: minSDKVersion has been upgraded to version 24

### 6.18.0

Android:

- Updated ZoomSDK to 5.13.10.12577
- BREAKING CHANGE: minSdkVersion has upgraded to version 23 (minimum platform version: Android 6.0)

### 6.17.1

Simplify installation for iOS and Android after using react-native@0.72.2

### 6.17.0

iOS updates:

- Updated ZoomSDK to 5.13.10.7064
- Delete `userId` param in `startMeeting`

### 6.16.5

Android:

- Use jitpack again - no need for applying breaking changes on 6.16.0

### 6.16.0

ANDROID BREAKING CHANGES:

- Add manually `commonlib.aar` and `mobilertc.aar`. See [Getting started](https://github.com/mieszko4/react-native-zoom-us/tree/feat/upgrade-android-to-5.13.1.11014#getting-started)
- Increased compileSdkVersion to 33
- Increased targetSdkVersion to 33
  Android updates:
- Update ZoomSDK to 5.13.1.11014
- Add new required listeners
  onSinkPanelistChatPrivilegeChanged(InMeetingChatController.MobileRTCWebinarPanelistChatPrivilege privilege)
  onShareMeetingChatStatusChanged(boolean start)
- Change deprecated listeners in favour of:
  - onSpotlightVideoChanged(boolean on) -> onSpotlightVideoChanged(List<Long> userList)
- Delete `userId` param in `startMeeting`

### 6.15.0

- Add `disableMinimizeMeeting` for `initialize` (iOS only)
- Add `disableClearWebKitCache` for `initialize`
- Fix `MeetingSettings` change not working in `initialize`
- Fix `autoConnectAudio` not working on iOS

### 6.13.0

Android updates:

- Add more logs
- Clean up passing initializePromise and meetingPromise, clean up to use only one initialize() call, use reactContext.getCurrentActivity()
- Wrap methods in try/catch
- Make sure to execute ZoomSDK on the main thread only
- Make `leaveMeeting` return promise
- Add `noMeetingErrorMessage` to `startMeeting` params
- On destroy unregister listeners and leave meeting
- On foreground register listeners and return to the meeting

### 6.12.0

iOS updates:

- Updated ZoomSDK to 5.11.0.3907

### 6.11.0

iOS updates:

- Updated ZoomSDK to 5.10.3.3244
- Changed deprecated listeners in favour of:
  - `(void)onSinkUserNameChanged:(NSUInteger)userID userName:(NSString *_Nonnull)userName -> (void)onSinkUserNameChanged:(NSArray <NSNumber*>* _Nullable)userNameChangedArr`
  - `(void)onMeetingCoHostChange:(NSUInteger)userId -> (void)onMeetingCoHostChange:(NSUInteger)userId isCoHost:(BOOL)isCoHost`

### 6.9.0

Android updates:

- Updated ZoomSDK to 5.10.3.5614
- Increased compileSdkVersion to 31
- Increased targetSdkVersion to 31
- Increased minSdkVersion to 21
- Increased buildToolsVersion to 30.0.2
- Added new required listeners
  - onLocalVideoOrderUpdated(List<Long> localOrderList)
  - onAllHandsLowered()
  - onPermissionRequested(String[] permissions)
  - onChatMsgDeleteNotification(String msgID, ChatMessageDeleteType deleteBy)
- Changed deprecated listeners in favour of:
  - onLocalRecordingStatus(RecordingStatus recordingStatus) -> onLocalRecordingStatus(long userId, RecordingStatus recordingStatus)
  - onUserNameChanged(long userId, String name) -> onUserNamesChanged(List<Long> userList)
  - onUserNetworkQualityChanged(long userId) -> onSinkMeetingVideoQualityChanged(VideoQuality videoQuality, long userId)
  - onMeetingCoHostChanged(long userId) -> onMeetingCoHostChange(long userId, boolean isCoHost)

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
  - `onSinkSharingStatus:(MobileRTCSharingStatus)status userID:(NSUInteger)userID`
  - `onVideoOrderUpdated:(NSArray <NSNumber *>* _Nullable)orderArr`
  - `onFollowHostVideoOrderChanged:(BOOL)follow`
    `
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
