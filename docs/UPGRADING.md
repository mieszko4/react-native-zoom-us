
## Upgrading guide


### 4.0.0

All IOS native dependencies are linking automatically.

So you need to remove all old references:
- MobileRTC.framework
- MobileRTCResources.bundle
- RNZoomUs.xcodeproj


Ideally your *.xcodeproj shouldn't match `react-native-zoom-us` now.

