
package ch.milosz.reactnative;

import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitializeListener;

import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;

public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, MeetingServiceListener, LifecycleEventListener {

  private final static String TAG = "RNZoomUs";
  private final ReactApplicationContext reactContext;

  private Boolean isInitialized = false;
  private Promise initializePromise;
  private Promise meetingPromise;

  public RNZoomUsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
  }

  @Override
  public String getName() {
    return "RNZoomUs";
  }

  @ReactMethod
  public void initialize(final String appKey, final String appSecret, final String webDomain, final Promise promise) {
    if (isInitialized) {
      promise.resolve("Already initialize Zoom SDK successfully.");
      return;
    }

    isInitialized = true;

    try {
      initializePromise = promise;

      reactContext.getCurrentActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ZoomSDK zoomSDK = ZoomSDK.getInstance();
            zoomSDK.initialize(reactContext.getCurrentActivity(), appKey, appSecret, webDomain, RNZoomUsModule.this);
          }
      });
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @Override
  public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
    Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
    if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
      initializePromise.reject(
              "ERR_ZOOM_INITIALIZATION",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
    } else {
      registerListener();
      initializePromise.resolve("Initialize Zoom SDK successfully.");
    }
  }

  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

    if(meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
      meetingPromise.reject(
              "ERR_ZOOM_MEETING",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
    } else if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
      meetingPromise.resolve("Connected to zoom meeting");
    }
  }

  private void registerListener() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    MeetingService meetingService = zoomSDK.getMeetingService();
    if(meetingService != null) {
      meetingService.addListener(this);
    }
  }

  private void unregisterListener() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    if(zoomSDK.isInitialized()) {
      MeetingService meetingService = zoomSDK.getMeetingService();
      meetingService.removeListener(this);
    }
  }

  // React LifeCycle
  @Override
  public void onHostDestroy() {
    unregisterListener();
  }
  @Override
  public void onHostPause() {}
  @Override
  public void onHostResume() {}
}
