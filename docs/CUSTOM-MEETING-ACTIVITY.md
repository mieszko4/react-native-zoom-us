# Extending MeetingActivity

*Currently only support for Android!*

If you have custom conference activity, instead official activity or custom UI.

Steps:
1. Create custom activity that extends `MeetingActivity`, e.g. `MyMeetingActivity`
2. Plug in your activity in `android/app/src/main/res/values/config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="zm_config_conf_activity">MyMeetingActivity</string>
</resources>
```

3. In order to make it visible define it in `android/app/src/main/AndroidManifest.xml`:
```xml
  <activity
    android:name="MyMeetingActivity"
    android:configChanges="orientation|screenSize"
    android:hardwareAccelerated="false"
  />
```

See [docs](https://marketplace.zoom.us/docs/sdk/native-sdks/android/mastering-zoom-sdk/in-meeting-function/customized-meeting-ui/overview) for more details.

## Backwards Button

On default backwards button exits the app.

You can change this behavior to exit the meeting.
In order to do this, instead of `MyMeetingActivity`, use `ch.milosz.reactnative.RNZoomUsActivity` included in the lib.