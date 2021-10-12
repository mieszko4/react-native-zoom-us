
## Size Reduction Tips [Android]

- Following the below steps will help you to reduce the app size by over 60%.


1. First compress your asset files (png, jpg, gif), use any online free image compressor website to compress it.
2. Enable [hermes engine](https://reactnative.dev/docs/hermes)
3. Add these [proguard-rules-zoom](https://gist.github.com/Md-Mudassir/0e0728e40c0149c74863ebde8066406e) -> `android/app/proguard-rules.pro`
4. Open `AndroidManifest.xml`
and inside `application` tag 

```xml
<application
 ...
 android:extractNativeLibs="true" //ADD THIS LINE
 ...
>
 ...
</application>
```
5. Go to `android/app/build.gradle` & enable 

```gradle
def enableSeparateBuildPerCPUArchitecture = true
def enableProguardInReleaseBuilds = true
```

> Proguard shrinks, optimizes and obfuscates Java code. It is able to optimize bytecode as well as detect and remove unused instructions. 

Now add the lines wherever I've mentioned `//ADD THIS LINE`
```gradle
android {
    ...

    defaultConfig {
        applicationId "com.zoom"
        minSdkVersion rootProject.ext.minSdkVersion
        targetSdkVersion rootProject.ext.targetSdkVersion
        versionCode 18
        resConfigs "en" //ADD THIS LINE 
        versionName "1.1.7"
        multiDexEnabled true
    }

    ...

    buildTypes {
        debug {
            signingConfig signingConfigs.debug
        }
        release {
            // Caution! In production, you need to generate your own keystore file.
            // see https://reactnative.dev/docs/signed-apk-android.
            shrinkResources true //ADD THIS LINE
            minifyEnabled true //ADD THIS LINE
            signingConfig signingConfigs.debug
            signingConfig signingConfigs.release
            minifyEnabled enableProguardInReleaseBuilds
            proguardFiles getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"
        }
    }
}

dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation 'com.facebook.soloader:soloader:0.9.0+' //ADD THIS LINE
    
    ...
}
```

## Size Reduction Tips [iOS]

For iOS follow this: [ios-reduce-your-app-size-with-app-thinning](https://agostini.tech/2019/06/02/reduce-your-app-size-with-app-thinning/)

> Make sure to set `ENABLE_BITCODE = NO;` for both Debug and Release because bitcode is not supported by Zoom iOS SDK


## References
- [zoom-reduce-app-size](https://devforum.zoom.us/t/apk-size-is-increased-after-integrate-zoom-sdk-from-20-mb-to-90-mb/5279/8?u=careerlabs)
- [enable  extractNativeLibs](https://devforum.zoom.us/t/apk-size-after-adding-android-zoom-sdk/16573/25?u=careerlabs)
- [How we reduced our production apk size by 70% in React Native?](https://dev.to/srajesh636/how-we-reduced-our-production-apk-size-by-70-in-react-native-1lci)
- [Reduce/Optimize React Native App Size](https://www.youtube.com/watch?v=W7boJmA7xJA&t=426)