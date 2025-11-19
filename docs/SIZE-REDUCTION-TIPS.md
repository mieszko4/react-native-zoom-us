
## Size Reduction Tips [Android]

Following the below steps will help you to reduce the app size by over 60%.


1. Make sure you compressed your asset files (png, jpg, gif). Use any online free image compressor website to compress it
2. Make sure you enabled [hermes engine](https://reactnative.dev/docs/hermes)
3. Apply proguard

Update the rules in `android/app/proguard-rules.pro`:
* Apply react-native and hermes: https://github.com/facebook/react-native/blob/v0.79.7/packages/react-native/ReactAndroid/proguard-rules.pro (note: adjust react-native version in the link)
* Apply Zoom SDK: [/android/proguard.cfg](/android/proguard.cfg)
* Make sure to also apply proguard rules for `react-native-*` libs that you use, e.g. for `react-native-svg` -> `-keep public class com.horcrux.svg.** {*;}`

Go to `android/app/build.gradle` and enable it:
```gradle
def enableProguardInReleaseBuilds = true
```

> Proguard shrinks, optimizes and obfuscates Java code. It is able to optimize bytecode as well as detect and remove unused instructions. 

4. Apply separate build per CPU Architecture

Go to `android/app/build.gradle` and enable it:
```gradle
def enableSeparateBuildPerCPUArchitecture = true
```

5. Extract native libs [deprecated](https://developer.android.com/build/releases/past-releases/agp-3-6-0-release-notes#extractNativeLibs)

Open `AndroidManifest.xml` and add inside `application` tag the following:
```xml
<application
 ...
 android:extractNativeLibs="true"
 ...
>
 ...
</application>
```

6. Shrink resources

Go to `android/app/build.gradle and enable it:
```gradle
android {
    ...
    buildTypes {
        ...
        release {
            ...
            shrinkResources true
        }
        ...
    }
    ...
}
```

7. Specify the languages your app supports

Go to `android/app/build.gradle and set it:
```gradle
android {
    defaultConfig {
        ...
        resConfigs "en"
    }
}
```

## Fix Android App crash after bundle release to Play Store [Android]
If you're running `/gradlew bundleRelease` to release your app on Playstore then you need to disable progaurd
```js
def enableProguardInReleaseBuilds = false
```
& rest of the above settings will remain the same & once progaurd is disabled then rebuild the app and upload the bundle file to playstore again which will fix the crash.
 
> Google Play uses your app bundle to generate and serve optimized APKs for each device configuration, so only the code and resources that are needed for a specific device are downloaded to run your app. You no longer have to build, sign, and manage multiple APKs to optimize support for different devices, and users get smaller, more-optimized downloads.

See diff for the example app: https://github.com/mieszko4/react-native-zoom-us-test/pull/33

## Size Reduction Tips [iOS]

For iOS follow this: [ios-reduce-your-app-size-with-app-thinning](https://agostini.tech/2019/06/02/reduce-your-app-size-with-app-thinning/)

> Make sure to set `ENABLE_BITCODE = NO;` for both Debug and Release because bitcode is not supported by Zoom iOS SDK


## References
- [zoom-reduce-app-size](https://devforum.zoom.us/t/apk-size-is-increased-after-integrate-zoom-sdk-from-20-mb-to-90-mb/5279/8?u=careerlabs)
- [enable  extractNativeLibs](https://devforum.zoom.us/t/apk-size-after-adding-android-zoom-sdk/16573/25?u=careerlabs)
- [How we reduced our production apk size by 70% in React Native?](https://dev.to/srajesh636/how-we-reduced-our-production-apk-size-by-70-in-react-native-1lci)
- [Reduce/Optimize React Native App Size](https://www.youtube.com/watch?v=W7boJmA7xJA&t=426)
