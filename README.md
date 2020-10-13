
# react-native-zoom-us

This is a bridge for ZoomUS SDK:
- android: https://github.com/zoom/zoom-sdk-android
- ios: https://github.com/zoom/zoom-sdk-ios

Pull requests are welcome.

- [Example](https://github.com/mieszko4/react-native-zoom-us-test)
- [Upgrading Guide](https://github.com/mieszko4/react-native-zoom-us/tree/master/docs/UPGRADING.md)

## Getting started

`$ npm install react-native-zoom-us`

### Installation

Library will be linked automatically. 

If you have `react-native < 0.60`, check [Linking Guide](https://github.com/mieszko4/react-native-zoom-us/tree/master/docs/LINKING.md)

#### iOS
Make sure you have appropriate description in Info.plist:
* `NSCameraUsageDescription`
* `NSMicrophoneUsageDescription`
* `NSPhotoLibraryUsageDescription`

## Usage
```typescript
import ZoomUs from 'react-native-zoom-us';

await ZoomUs.initialize({
  clientKey: '...',
  clientSecret: '...',
})

// Start Meeting
await ZoomUs.startMeeting({
  userName: 'Johny',
  meetingNumber: '12345678',
  userId: 'our-identifier',
  zoomAccessToken: zak,
  userType: 2, // optional
})

// Join Meeting
await ZoomUs.joinMeeting({
  userName: 'Johny',
  meetingNumber: '12345678',
})

// Join Meeting with extra params
await ZoomUs.joinMeeting({
  userName: 'Johny',
  meetingNumber: '12345678',
  password: '1234',
  participantID: 'our-unique-id',
  noAudio: true,
  noVideo: true,
})
```


## FAQ

#### Does library support Expo?
You have to eject your expo project to use this library.
