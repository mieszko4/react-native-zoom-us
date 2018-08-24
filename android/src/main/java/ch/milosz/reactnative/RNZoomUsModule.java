
package ch.milosz.reactnative;

import java.lang.Math;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class RNZoomUsModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNZoomUsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  /**
   * PUBLIC REACT API
   *
   *  getRandomNumber()   Returns a random number
   */
  @ReactMethod
  public void getRandomNumber(final Promise promise) {
    try {
      double n = Math.random();
      promise.resolve(n);
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @Override
  public String getName() {
    return "RNZoomUs";
  }
}
