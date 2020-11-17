
# react-native-zoom-us

This is a bridge for ZoomUS SDK:
- android: https://github.com/zoom/zoom-sdk-android
- ios: https://github.com/zoom/zoom-sdk-ios

Tested on XCode 11.5 and node 12.18.1.

Pull requests are welcome.

- [Example](https://github.com/mieszko4/react-native-zoom-us-test)
- [Upgrading Guide](https://github.com/mieszko4/react-native-zoom-us/tree/master/docs/UPGRADING.md)

## Getting started

`$ npm install react-native-zoom-us`

### Installation

If you have `react-native < 0.60`, check [Full Linking Guide](https://github.com/mieszko4/react-native-zoom-us/tree/master/docs/LINKING.md)

#### Android

1. Add repository to `android/build.gradle`:
```gradle
allprojects {
    repositories {
        flatDir {
            dirs "$rootDir/../node_modules/react-native-zoom-us/android/libs"
        }
    }
}   
```

2. Set `minSdkVersion` to `21`
```gradle
buildscript {
    ext {
        minSdkVersion = 21
    }
}
```

See [diff](https://github.com/mieszko4/react-native-zoom-us-test/pull/10/commits/cabdb876cc40f78f0a6d977d38377497be5e0726) for reference.

#### iOS
Make sure you have appropriate description in Info.plist:
* `NSCameraUsageDescription`
* `NSMicrophoneUsageDescription`
* `NSPhotoLibraryUsageDescription`

## Usage
```typescript
import ZoomUs from 'react-native-zoom-us';

// initialize minimal
await ZoomUs.initialize({
  clientKey: '...',
  clientSecret: '...',
})

// initialize with extra config
await ZoomUs.initialize({
  clientKey: '...',
  clientSecret: '...',
  domain: 'zoom.us'
}, {
  disableShowVideoPreviewWhenJoinMeeting: true
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
