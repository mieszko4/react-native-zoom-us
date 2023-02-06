
# react-native-zoom-us

This is a bridge for ZoomUS SDK.

[![npm](https://img.shields.io/npm/v/react-native-zoom-us)](https://www.npmjs.com/package/react-native-zoom-us)

| Platform | Version     | SDK Url                                                                 |                                          Changelog                                          |
|:--------:|:------------| :----------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------:|
|   iOS    | 5.11.3.4099 | [ZoomSDK](https://github.com/zoom-us-community/zoom-sdk-pods)           | [marketplace.zoom.us](https://marketplace.zoom.us/docs/changelog#labels/client-sdk-i-os)    |
| Android  | 5.13.1.11014| Added manually, see "Getting started" below                             | [marketplace.zoom.us](https://marketplace.zoom.us/docs/changelog#labels/client-sdk-android) |

Tested on Android and iOS: ([See details](https://github.com/mieszko4/react-native-zoom-us#testing))

Pull requests are welcome.

- [Example](https://github.com/mieszko4/react-native-zoom-us-test)
- [Upgrading Guide](./docs/UPGRADING.md)
- [CHANGELOG](./CHANGELOG.md)
- [TROUBLESHOOTING](./docs/TROUBLESHOOTING.md)

## Docs

- [Screenshare on iOS](docs/IOS-SCREENSHARE.md)
- [Events](docs/EVENTS.md)
- [Video View Component](docs/VIDEO-VIEW.md)
- [Size Reduction](docs/SIZE-REDUCTION-TIPS.md)
- [Custom Meeting Activity](docs/CUSTOM-MEETING-ACTIVITY.md)


## Getting started

1. Install npm lib
Run `npm install react-native-zoom-us`

2. [Android only] Get Meeting SDK with *.aar files

The following manual steps are needed because ZoomUs SDK does not expose *.aar files directly.

* Register on ZoomUs (https://zoom.us/signup#/signup)
* Go to https://marketplace.zoom.us/develop/create
* Click on `Create` placed on `Meeting SDK` card, choose `App Name` and click on `Continue`
* Fill in `Company Name`, `Name` and `Email address` and click on `Continue`
* On `Download` step choose `Android` then `v5.13.1.11014` and click on download icon
* Uzip the content of downloaded `zoom-sdk-android-5.13.1.11014.zip`
* Copy two *.aar files to the `node_modules/react-native-zoom-us` folder of your project:
- `cp zoom-sdk-android-5.13.1.11014/mobilertc-android-studio/commonlib/commonlib.aar node_modules/react-native-zoom-us/android/commonlib`
- `cp zoom-sdk-android-5.13.1.11014/mobilertc-android-studio/mobilertc/mobilertc.aar node_modules/react-native-zoom-us/android/mobilertc`


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

2. Declare permissions

Depending on how you will use the lib, you will need to declare permissions in /android/app/src/main/AndroidManifest.xml.
This is the minimum set of permissions you need to add in order to use audio and video:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
  <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
  <uses-permission android:name="android.permission.CAMERA"/>
  <uses-permission android:name="android.permission.RECORD_AUDIO"/>
  
  ...
</manifest>
```

3. Add this to /android/app/src/debug/AndroidManifest.xml
```xml
<application
  ...
  tools:remove="android:networkSecurityConfig"
  tools:replace="android:usesCleartextTraffic"
>
```
This is needed because ZoomSDK declares `android:networkSecurityConfig`


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

Note that M1 chip is not supported by Zoom SDK.
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
  userId: 'our-identifier', // ios only field
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

## Events Api

Hook sample for listening events:
```tsx
import ZoomUs from 'react-native-zoom-us'

useEffect(() => {
  const listener = ZoomUs.onMeetingStatusChange(({ event }) => {
    console.log('onMeetingStatusChange', event)
  })
  const joinListener = ZoomUs.onMeetingJoined(() => {
    console.log('onMeetingJoined')
  })
  
  return () => {
    listener.remove()
    joinListener.remove()
  }
}, [])
```

If you need more events, take a look [Events](./docs/EVENTS.md)


## Testing

The plugin has been tested for `joinMeeting` using [smoke test procedure](https://github.com/mieszko4/react-native-zoom-us-test#smoke-test-procedure):
* react-native-zoom-us: 6.15.1
* react-native: 0.70.5
* node: 16.18.1
* macOS: 13.0
* XCode: 14.1
* Android minSdkVersion: 21


## FAQ

#### Does library support Expo?
You have to eject your expo project to use this library.
