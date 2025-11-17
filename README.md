# react-native-zoom-us

This is a bridge for [ZoomUS Meeting SDK](https://developers.zoom.us/docs/meeting-sdk/).

`NOTE`: In August 2024, [official bridge](https://developers.zoom.us/docs/meeting-sdk/react-native/) has been released.

[![npm](https://img.shields.io/npm/v/react-native-zoom-us)](https://www.npmjs.com/package/react-native-zoom-us)

| Platform | Version       |                                 SDK Url                                           |                                          Changelog                                          |
| :------: | :------------ | :-------------------------------------------------------------------------------: | :-----------------------------------------------------------------------------------------: |
|   iOS    | 6.5.10.27930  | [ZoomSDK](https://github.com/zoom-us-community/zoom-sdk-pods)                     |  [marketplace.zoom.us](https://developers.zoom.us/changelog/meeting-sdk/ios/)               |
| Android  | 6.5.10        | [Zoom Meeting SDK](https://mvnrepository.com/artifact/us.zoom.meetingsdk/zoomsdk) |  [marketplace.zoom.us](https://developers.zoom.us/changelog/meeting-sdk/android/)           |

Tested on Android and iOS: ([See details](https://github.com/mieszko4/react-native-zoom-us#testing))

Pull requests are welcome.

- [Example](https://github.com/mieszko4/react-native-zoom-us-test)
- [Upgrading Guide](./docs/UPGRADING.md)
- [CHANGELOG](./CHANGELOG.md)
- [TROUBLESHOOTING](./docs/TROUBLESHOOTING.md)

## Docs

- [Screenshare on iOS](docs/IOS-SCREENSHARE.md)
- [Events](docs/EVENTS.md)
- [Size Reduction](docs/SIZE-REDUCTION-TIPS.md)
- [Custom Meeting Activity](docs/CUSTOM-MEETING-ACTIVITY.md)

## Getting started

Install npm lib: `npm install react-native-zoom-us`

### Installation

#### Android

1. Declare permissions

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

You may also need the following permissions:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>

  ...
</manifest>
```

2. Add this to /android/app/src/debug/AndroidManifest.xml

```xml
<application
  ...
  tools:remove="android:networkSecurityConfig"
  tools:replace="android:usesCleartextTraffic"
>
```

This is needed because ZoomSDK declares `android:networkSecurityConfig`

3. `npm run android`

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

3. `npm run ios`

## Usage

```typescript
import ZoomUs from 'react-native-zoom-us';

// initialize
await ZoomUs.initialize({
  jwtToken: '...',
});

// initialize with extra config
await ZoomUs.initialize(
  {
    jwtToken: '...',
    domain: 'zoom.us',
  },
  {
    disableShowVideoPreviewWhenJoinMeeting: true,
  },
);

// Start Meeting
await ZoomUs.startMeeting({
  userName: 'Johny',
  meetingNumber: '12345678',
  zoomAccessToken: zak,
  userType: 2, // optional
});

// Join Meeting
await ZoomUs.joinMeeting({
  userName: 'Johny',
  meetingNumber: '12345678',
});

// Join Meeting with extra params
await ZoomUs.joinMeeting({
  userName: 'Johny',
  meetingNumber: '12345678',
  password: '1234',
  noAudio: true,
  noVideo: true,
});

// Leave Meeting
await ZoomUs.leaveMeeting();

// Connect Audio
await ZoomUs.connectAudio();
// you can also use autoConnectAudio: true in `ZoomUs.joinMeeting`
```

## Events Api

Hook sample for listening events:

```tsx
import ZoomUs from 'react-native-zoom-us';

useEffect(() => {
  const listener = ZoomUs.onMeetingStatusChange(({event}) => {
    console.log('onMeetingStatusChange', event);
  });
  const joinListener = ZoomUs.onMeetingJoined(() => {
    console.log('onMeetingJoined');
  });

  return () => {
    listener.remove();
    joinListener.remove();
  };
}, []);
```

If you need more events, take a look [Events](./docs/EVENTS.md)

## Testing

The plugin has been tested for `joinMeeting` and `startMeeting` using [smoke test procedure](https://github.com/mieszko4/react-native-zoom-us-test#smoke-test-procedure):

- react-native-zoom-us: 14.0.0
- react-native: 0.77.3
- node: 18.20.7
- macOS: 26.0.1 M1
- XCode: 26.0
- iOS: 16.4 (simulator)
- iOS: 16.7 (iPhone 8)
- Android minSdkVersion: 26

## FAQ

#### Does library support Expo?

You have to eject your expo project to use this library.
