
# react-native-zoom-us

This is a bridge for ZoomUS SDK.

[![npm](https://img.shields.io/npm/v/react-native-zoom-us)](https://www.npmjs.com/package/react-native-zoom-us)

| Platform | Version     | SDK Url                                                                 |                                          Changelog                                          |
|:--------:|:------------| :----------------------------------------------------------------------:|:-------------------------------------------------------------------------------------------:|
|   iOS    | 5.11.3.4099 | [ZoomSDK](https://github.com/zoom-us-community/zoom-sdk-pods)           | [marketplace.zoom.us](https://marketplace.zoom.us/docs/changelog#labels/client-sdk-i-os)    |
| Android  | 5.12.2.9109 | [jitpack-zoom-us](https://github.com/zoom-us-community/jitpack-zoom-us) | [marketplace.zoom.us](https://marketplace.zoom.us/docs/changelog#labels/client-sdk-android) |

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

4. Add this to /android/app/src/main/res/xml/network_security_config.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">ocsp.digicert.com</domain>
    <domain includeSubdomains="true">crl3.digicert.com</domain>
    <domain includeSubdomains="true">crl4.digicert.com</domain>
    <domain includeSubdomains="true">crl.godaddy.com</domain>
    <domain includeSubdomains="true">certificates.godaddy.com</domain>
    <domain includeSubdomains="true">crl.starfieldtech.com</domain>
    <domain includeSubdomains="true">certificates.starfieldtech.com</domain>
    <domain includeSubdomains="true">ocsp.godaddy.com</domain>
    <domain includeSubdomains="true">ocsp.starfieldtech.com</domain>
  </domain-config>
</network-security-config>
```
Then add this to /android/app/src/main/AndroidManifest.xml
```xml
<application
  ...

  android:networkSecurityConfig="@xml/network_security_config"
>
```

Source: https://8xmdmkir8ctlkfj8dttx.noticeable.news/publications/android-meeting-sdk-v5-9-0.

5. Add this to /android/app/src/debug/res/xml/network_security_config.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <!-- deny cleartext traffic for React Native packager ips in release -->
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">localhost</domain>
    <domain includeSubdomains="true">10.0.2.2</domain>
    <domain includeSubdomains="true">10.0.3.2</domain>
  </domain-config>
</network-security-config>
```
Then add this to /android/app/src/debug/AndroidManifest.xml
```xml
<application
  ...

  tools:replace="android:usesCleartextTraffic"
  android:networkSecurityConfig="@xml/network_security_config"
>
```

6. Declare permissions

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
* react-native-zoom-us: 6.9.0
* react-native: 0.66.0
* node: 16.14.2
* macOS: 12.4
* XCode: 13.3.1
* Android minSdkVersion: 21


## FAQ

#### Does library support Expo?
You have to eject your expo project to use this library.
