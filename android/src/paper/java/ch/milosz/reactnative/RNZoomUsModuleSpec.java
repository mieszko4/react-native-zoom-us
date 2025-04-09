package ch.milosz.reactnative;

import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class RNZoomUsModuleSpec extends ReactContextBaseJavaModule implements TurboModule {
    public static final String NAME = "RNZoomUs";

    public RNZoomUsModuleSpec(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public @Nonnull String getName() {
        return NAME;
    }

    @ReactMethod
    @DoNotStrip
    public abstract void isInitialized(Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void initialize(final ReadableMap params, final ReadableMap settings, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void addVideoView(final int tagId, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void removeVideoView(final int tagId, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void startMeeting(final ReadableMap paramMap, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void joinMeeting(final ReadableMap paramMap, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void leaveMeeting(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void connectAudio(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void isMeetingConnected(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void isMeetingHost(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void getInMeetingUserIdList(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void muteMyVideo(final boolean muted, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void rotateMyVideo(final int rotation, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void muteMyAudio(final boolean muted, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void muteAttendee(final String userId, final boolean muted, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void muteAllAttendee(final boolean allowUnmuteSelf, final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void startShareScreen(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void stopShareScreen(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void switchCamera(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void raiseMyHand(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void lowerMyHand(final Promise promise);

    @ReactMethod
    @DoNotStrip
    public abstract void addListener(String eventName);

    @ReactMethod
    @DoNotStrip
    public abstract void removeListeners(Integer count);
}
