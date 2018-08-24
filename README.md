
# react-native-zoom-us

## Getting started

`$ npm install react-native-zoom-us --save`

### Mostly automatic installation

`$ react-native link react-native-zoom-us`

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
import RNZoomUs from 'react-native-zoom-us';

// TODO: What to do with the module?
RNZoomUs;
```
  