
## Upgrading guide

### 6.0.0 (JitPack)

- You should remove a local repository from build.gradle:
```gradle
flatDir {
    dirs "$rootDir/../node_modules/react-native-zoom-us/android/libs"
}
```

- Set `pickFirst` rules

- ZoomSDk now uses exoplayer. So you can experience problems if it already exists in project.
You can try to disable it: 
```gradle
implementation (project(':react-native-zoom-us')) {
    exclude group: 'com.google.android.exoplayer'
}
```
or check troubleshooting docs for extra options


### 5.0.0 (TypeScript)
You can check notes there: https://github.com/mieszko4/react-native-zoom-us/pull/31

Methods requires objects now, so you have to update them. 
Check next methods:

#### ZoomUs.initialize

before:
```javascript
await ZoomUs.initialize(
  config.zoom.appKey,
  config.zoom.appSecret,
  config.zoom.domain
);
```

after:
```typescript
await ZoomUs.initialize({
  clientKey: '...',
  clientSecret: '...',
  domain: 'zoom.us',
})
```

#### ZoomUs.startMeeting

before:
```javascript
await ZoomUs.startMeeting(
  displayName,
  meetingNo,
  userId,
  userType,
  zoomAccessToken, 
  zoomToken
);
```

after:
```typescript
await ZoomUs.startMeeting({
  userName: displayName,
  meetingNumber: meetingNo,
  userId: userId,
  zoomAccessToken: zoomAccessToken,
  userType: userType,
})
```

#### ZoomUs.joinMeeting

before: 
```javascript
await ZoomUs.joinMeeting(
  displayName,
  meetingNo
);
```

after:
```typescript
ZoomUs.joinMeeting({
  userName: displayName,
  meetingNumber: meetingNo,
})
```


### 4.0.0

All IOS native dependencies are linking automatically.

So you need to remove all old references:
- MobileRTC.framework
- MobileRTCResources.bundle
- RNZoomUs.xcodeproj


Ideally your *.xcodeproj shouldn't match `react-native-zoom-us` now.

