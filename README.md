
# react-native-zoom-us

## Getting started

`$ npm install react-native-zoom-us --save`

### Mostly automatic installation

`$ react-native link react-native-zoom-us`

#### Android

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

Note: In `android/app/build.gradle` I tried to set up `compile project(':react-native-zoom-us')` with `transitive=false`
and it compiled well, but the app then crashes after running with initialize/meeting listener.
So the above solution seems to be the best for now.


### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-zoom-us` and add `RNZoomUs.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNZoomUs.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

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


## Usage
```javascript
import ZoomUs from 'react-native-zoom-us';

await ZoomUs.initialize(
  config.zoom.appKey,
  config.zoom.appSecret,
  config.zoom.domain
);

// Start Meeting
await ZoomUs.startMeeting(
  displayName,
  meetingNo,
  userId, // can be 'null'?
  userType, // for pro user use 2
  zoomAccessToken, // zak token
  zoomToken // can be 'null'?

  // NOTE: userId, userType, zoomToken should be taken from user hosting this meeting (not sure why it is required)
  // But it works with putting only zoomAccessToken
);

// OR Join Meeting
await ZoomUs.joinMeeting(
  displayName,
  meetingNo
);
```
