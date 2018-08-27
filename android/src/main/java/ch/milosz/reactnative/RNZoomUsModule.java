
package ch.milosz.reactnative;

import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitializeListener;

public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener {

  private final static String TAG = "RNZoomUs";
  private final ReactApplicationContext reactContext;

  private ZoomSDK mZoomSDK;
  private Boolean isInitialized = false;
  private Promise initializePromise;

  public RNZoomUsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
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
            mZoomSDK = ZoomSDK.getInstance();
            mZoomSDK.initialize(reactContext.getCurrentActivity(), appKey, appSecret, webDomain, RNZoomUsModule.this);
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
              "Failed to initialize Zoom SDK. Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
    } else {
      initializePromise.resolve("Initialize Zoom SDK successfully.");
    }
  }
}
