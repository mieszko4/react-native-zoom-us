
## Upgrading guide

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

