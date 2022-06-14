

### exoplayer version conflict

If you see error like this: 
```
java.lang.NoClassDefFoundError: Failed resolution of: Lcom/google/android/exoplayer2/drm/DefaultDrmSessionEventListener
```

You can experience it with `react-native-video:5.1.1`.

You can solve it in 2 ways:

1) Disable exoplayer for this lib:
```gradle
implementation (project(':react-native-zoom-us')) {
    exclude group: 'com.google.android.exoplayer'
}
```

2) Use newer version of react-native-video:5.2.0 or higher.
If lib still not published: `yarn add https://github.com/react-native-video/react-native-video#724b8629f6c7f222c08e60e6948d06fa45a6f4f2`

Note: it can be also with other libs which uses exoplayer

**Note:** Solution can be different for newer versions.
