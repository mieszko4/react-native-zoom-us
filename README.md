
# react-native-zoom-us

This is a bridge for ZoomUS SDK:

| Platform      | Version           | Url                                      | Changelog                                                            |
| :-----------: | :---------------  | :--------------------------------------: | :------------------------------------------------------------------: |
| iOS	        | 5.7.1.645         | https://github.com/zoom/zoom-sdk-ios     | https://marketplace.zoom.us/docs/changelog#labels/client-sdk-i-os    |
| Android       | 5.7.1.1268        | https://github.com/zoom/zoom-sdk-android | https://marketplace.zoom.us/docs/changelog#labels/client-sdk-android |

Tested on XCode 12.4 and react-native 0.66.0. ([See details](https://github.com/mieszko4/react-native-zoom-us#testing))

Pull requests are welcome.

- [Example](https://github.com/mieszko4/react-native-zoom-us-test)
- [Upgrading Guide](./docs/UPGRADING.md)
- [CHANGELOG](./CHANGELOG.md)
- [TROUBLESHOOTING](./docs/TROUBLESHOOTING.md)

## Getting started

`$ npm install react-native-zoom-us`

### Installation

If you have `react-native < 0.60`, check [Full Linking Guide](docs/LINKING.md)

#### Android

1. Set `pickFirst` rules in `android/app/build.gradle`

```gradle
android {
    packagingOptions {
        pickFirst 'lib/arm64-v8a/libc++_shared.so'
        pickFirst 'lib/x86/libc++_shared.so'
        pickFirst 'lib/x86_64/libc++_shared.so'
        pickFirst 'lib/armeabi-v7a/libc++_shared.so'
    }
}
```

2. In your `MainApplication.java` inside of `onCreate` add `SoLoader.loadLibrary("zoom");`:

```java
@Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, /* native exopackage */ false);
    SoLoader.loadLibrary("zoom"); // <-- ADD THIS LINE
    initializeFlipper(this, getReactNativeHost().getReactInstanceManager());
  }
```

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

4. Optional: Implement custom UI
See [docs](https://marketplace.zoom.us/docs/sdk/native-sdks/iOS/mastering-zoom-sdk/in-meeting-function/customized-in-meeting-ui/overview) for more details.

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
  disableShowVideoPreviewWhenJoinMeeting: true,
  enableCustomizedMeetingUI: true
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
  noAudio: true,
  noVideo: true,
})

// Leave Meeting
await ZoomUs.leaveMeeting()

// Connect Audio
await ZoomUs.connectAudio()
// you can also use autoConnectAudio: true in `ZoomUs.joinMeeting`
```

## Docs

- [Screenshare on iOS](docs/IOS-SCREENSHARE.md)
- [Events](docs/EVENTS.md)
- [Video View Component](docs/VIDEO-VIEW.md)
- [Size Reduction](docs/SIZE-REDUCTION-TIPS.md)
- [Custom Meeting Activity](docs/CUSTOM-MEETING-ACTIVITY.md)

## Testing

The plugin has been tested for `joinMeeting` using [smoke test procedure](https://github.com/mieszko4/react-native-zoom-us-test#smoke-test-procedure):
* react-native-zoom-us: 6.5.1
* react-native: 0.66.0
* node: 14.16.0
* macOS: 10.15.5
* XCode: 12.4
* android minSdkVersion: 21


## FAQ

#### Does library support Expo?
You have to eject your expo project to use this library.
