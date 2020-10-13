## Linking for react-native < 0.60

### Mostly automatic installation

`$ react-native link react-native-zoom-us`

#### Extra steps for Android

Since Zoom SDK `*.aar` libraries are not globally distributed
it is also required to manually go to your project's `android/build.gradle` and under `allprojects.repositories` add the following:
```gradle
allprojects {
    repositories {
        flatDir {
            dirs "$rootDir/../node_modules/react-native-zoom-us/android/libs"
        }
        ...
    }
    ...
}
```

If you have problem with multiDex go to your project's `android/app/build.gradle` and under `android.defaultSettings` add the following:
```gradle
android {
    defaultConfig {
        multiDexEnabled true
        ...
    }
    ...
}
```

Note: In `android/app/build.gradle` I tried to set up `compile project(':react-native-zoom-us')` with `transitive=false`
and it compiled well, but the app then crashes after running with initialize/meeting listener.
So the above solution seems to be the best for now.

Note: If you're using proguard copy the contents of this module's android/proguard.cfg and append them to your project's android/app/proguard-rules.pro otherwise the release build will crash

#### Extra steps for iOS

1. In XCode, in your main project go to `General` tab, expand `Linked Frameworks and Libraries` and add the following libraries:
* `libsqlite3.tbd`
* `libstdc++.6.0.9.tbd`
* `libz.1.2.5.tbd`

2. In XCode, in your main project go to `General` tab, expand `Linked Frameworks and Libraries` and add `MobileRTC.framework`:
* choose `Add other...`
* navigate to `../node_modules/react-native-zoom-us/ios/libs`
* choose `MobileRTC.framework`

3. In XCode, in your main project go to `General` tab, expand `Embedded Binaries` and add `MobileRTC.framework` from the list - should be at `Frameworks`.

4. In XCode, in your main project go to `Build Phases` tab, expand `Copy Bundle Resources` and add `MobileRTCResources.bundle`:
* choose `Add other...`
* navigate to `../node_modules/react-native-zoom-us/ios/libs`
* choose `MobileRTCResources.bundle`
* choose `Create folder references` and uncheck `Copy files if needed`
Note: if you do not have `Copy Bundle Resources` you can add it by clicking on top-left `+` sign

5. In XCode, in your main project go to `Build Settings` tab:
* search for `Enable Bitcode` and make sure it is set to `NO`
* search for `Framework Search Paths` and add `$(SRCROOT)/../node_modules/react-native-zoom-us/ios/libs` with `non-recursive`

6. In XCode, in your main project go to `Info` tab and add the following keys with appropriate description:
* `NSCameraUsageDescription`
* `NSMicrophoneUsageDescription`
* `NSPhotoLibraryUsageDescription`

6. Because this package includes Zoom SDK that works for both simulator and real device, when releasing to app store you may encounter problem with unsupported architecure. Please follow this answer to add script in `Build Phases` that filters out unsupported architectures: https://stackoverflow.com/questions/30547283/submit-to-app-store-issues-unsupported-architecture-x86. You may want to modify the script to be more specific, i.e. replace `'*.framework'` with `'MobileRTC.framework'`.

### Manual installation

#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-zoom-us` and add `RNZoomUs.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNZoomUs.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<
5. Follow [Mostly automatic installation-> Extra steps for iOS](#extra-steps-for-ios)

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import ch.milosz.reactnative.RNZoomUsPackage;` to the imports at the top of the file
  - Add `new RNZoomUsPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-zoom-us'
  	project(':react-native-zoom-us').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-zoom-us/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-zoom-us')
  	```
4. Follow [Mostly automatic installation-> Extra steps for Android](#extra-steps-for-android)
