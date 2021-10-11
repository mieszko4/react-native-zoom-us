
## Size Reducion Tips

- Following the below steps will help you to reduce the app size by over 60%.


1. First compress your asset files (png, jpg, gif), use any online free image compressor website to compress it.
2. Open `AndroidManifest.xml`
and inside `application` tag 

```js
<application
 ...
 android:extractNativeLibs="true" //Add this line
 ...
</application>
```
3. Go to `android/app/build.gradle` & enable 

`def enableSeparateBuildPerCPUArchitecture = true`
`def enableProguardInReleaseBuilds = true`

> Proguard shrinks, optimizes and obfuscates Java code. It is able to optimize bytecode as well as detect and remove unused instructions. 

Now add the lines where ever i've mentioned `//Add this line`
```js
android {
    packagingOptions {
    pickFirst 'lib/x86/libc++_shared.so'
    pickFirst 'lib/x86_64/libc++_shared.so'
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
    pickFirst 'lib/arm64-v8a/libc++_shared.so'
}

    defaultConfig {
        applicationId "com.zoom"
        minSdkVersion rootProject.ext.minSdkVersion
        targetSdkVersion rootProject.ext.targetSdkVersion
        versionCode 18
        resConfigs "en" //Add this line 
        versionName "1.1.7"
        multiDexEnabled true
    }
    splits {
        abi {
            reset()
            enable enableSeparateBuildPerCPUArchitecture
            universalApk true  // If true, also generate a universal APK
            include "armeabi-v7a", "x86", "arm64-v8a", "x86_64"
        }
    }

    buildTypes {
        debug {
            signingConfig signingConfigs.debug
        }
        release {
            // Caution! In production, you need to generate your own keystore file.
            // see https://reactnative.dev/docs/signed-apk-android.
            shrinkResources true //Add this line
            minifyEnabled true //Add this line
            signingConfig signingConfigs.debug
            signingConfig signingConfigs.release
            minifyEnabled enableProguardInReleaseBuilds
            proguardFiles getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"
        }
    }

    //Add this below snippet
    applicationVariants.all { variant ->
        variant.outputs.each { output ->
            // For each separate APK per architecture, set a unique version code as described here:
            // https://developer.android.com/studio/build/configure-apk-splits.html
            def versionCodes = ["armeabi-v7a": 1, "x86": 2, "arm64-v8a": 3, "x86_64": 4]
            def abi = output.getFilter(OutputFile.ABI)
            if (abi != null) {  // null for the universal-debug, universal-release variants
                output.versionCodeOverride =
                        versionCodes.get(abi) * 1048576 + defaultConfig.versionCode
            }

        }
    }
}

```

For iOS follow this: [ios-reduce-your-app-size-with-app-thinning](https://agostini.tech/2019/06/02/reduce-your-app-size-with-app-thinning/)

> Make sure to set `ENABLE_BITCODE = NO;` for both Debug and Release because bitcode is not supported by Zoom iOS SDK


References : 
- [zoom-reduce-app-size](https://devforum.zoom.us/t/apk-size-is-increased-after-integrate-zoom-sdk-from-20-mb-to-90-mb/5279/8?u=careerlabs),
- [enable  extractNativeLibs](https://devforum.zoom.us/t/apk-size-after-adding-android-zoom-sdk/16573/25?u=careerlabs)
- [How we reduced our production apk size by 70% in React Native?](https://dev.to/srajesh636/how-we-reduced-our-production-apk-size-by-70-in-react-native-1lci)
- [Reduce/Optimize React Native App Size](https://www.youtube.com/watch?v=W7boJmA7xJA&t=426)