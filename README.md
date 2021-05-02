
# react-native-zoom-us

This is a bridge for ZoomUS SDK:

| Platform      | Version           | Url                                      | Changelog                                                            |
| :-----------: | :---------------: | :--------------------------------------: | :------------------------------------------------------------------: |
| iOS	          | 5.4.54802.0124    | https://github.com/zoom/zoom-sdk-ios     | https://marketplace.zoom.us/docs/changelog#labels/client-sdk-i-os    |
| Android       | 5.0.24433.0616    | https://github.com/zoom/zoom-sdk-android | https://marketplace.zoom.us/docs/changelog#labels/client-sdk-android |

Tested on XCode 12.4 and react-native 0.64.0. ([See details](https://github.com/mieszko4/react-native-zoom-us#testing))

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
1. Make sure you have appropriate description in `Info.plist`:
```xml
<key>NSBluetoothPeripheralUsageDescription</key>
<string>We will use your Bluetooth to access your Bluetooth headphones.</string>
	
<key>NSCameraUsageDescription</key>
<string>For people to see you during meetings, we need access to your camera.</string>
	
<key>NSMicrophoneUsageDescription</key>
<string>For people to hear you during meetings, we need access to your microphone.</string>
	
<key>NSPhotoLibraryUsageDescription</key>
<string>For people to share, we need access to your photos.</string>
```

2. Update pods using `cd ios/ && pod install && cd ..`

3. Make sure to set `ENABLE_BITCODE = NO;` for both Debug and Release because bitcode is not supported by Zoom iOS SDK 

## Usage
```typescript
import ZoomUs from 'react-native-zoom-us';

// initialize minimal
await ZoomUs.initialize({
  clientKey: '...',
  clientSecret: '...',
})

// initialize using JWT
await ZoomUs.initialize({
  jwtToken: '...',
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

## Docs

- [Screenshare on iOS](https://github.com/mieszko4/react-native-zoom-us/tree/master/docs/IOS-SCREENSHARE.md)
- [Events](https://github.com/mieszko4/react-native-zoom-us/tree/master/docs/EVENTS.md)


## Testing

The plugin has been tested for `joinMeeting` using [smoke test procedure]https://github.com/mieszko4/react-native-zoom-us-test#smoke-test-procedure:
* react-native-zoom-us: 5.8.1
* react-native: 0.64.0
* node: 14.16.0
* macOS: 10.15.5
* XCode: 12.4
* android minSdkVersion: 21


## FAQ

#### Does library support Expo?
You have to eject your expo project to use this library.
