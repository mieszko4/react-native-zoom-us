## Screenshare on iOS

Configuring screensharing on iOS is somewhat complex. In order to set it up, you will need to do the
following:

1. Follow the instructions from Zoom for setting up (Screen Broadcast with
ReplayKit)[https://marketplace.zoom.us/docs/sdk/native-sdks/iOS/mastering-zoom-sdk/in-meeting-function/screen-share#broadcast-device-screen]
(make sure to add the `MobileRTCScreenShare.framework` from the ZoomSDK pod in step 3)
2. Pass the Bundle ID you configured in step 3 of the Zoom instructions as `screenShareExtension`
to `ZoomUs.initialize`
3. Pass the App Group ID you configured in step 6 of the Zoom instructions as `appGroupId` to
`ZoomUs.initialize`

The resulting call to `ZoomUs.initialize` should look something like this:
```
await ZoomUs.initialize({
  clientKey: '...',
  cientSecret: '...',
  appGroupId: 'group.com.your.Bundle',
  screenShareExtension: 'com.your.Bundle.ScreenShare'
});
```