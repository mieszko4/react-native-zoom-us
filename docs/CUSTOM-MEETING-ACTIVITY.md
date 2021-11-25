# Extending MainActivity

*Currently only support for Android!*

If you have custom conference activity, instead official activity or custom UI.

Steps:
1. Create custom activity that extends `MainActivity`, e.g. `MyActivity`
2. Plug in your activity in `android/app/src/main/res/values/config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="zm_config_conf_activity">MyActivity</string>
</resources>
```

3. In order to make it visible define it in `android/app/src/main/AndroidManifest.xml`:
```xml
  <activity
    android:name="MyActivity"
    android:configChanges="orientation|screenSize"
    android:hardwareAccelerated="false"
  />
```

See [docs](https://marketplace.zoom.us/docs/sdk/native-sdks/android/mastering-zoom-sdk/in-meeting-function/customized-meeting-ui/overview) for more details.

## Backwards Button

On default backwards button exits the app.

You can change this behavior to exit the meeting.
In order to do this, instead of `MyActivity`, use `ch.milosz.reactnative.RNZoomUsActivity` included in the lib.