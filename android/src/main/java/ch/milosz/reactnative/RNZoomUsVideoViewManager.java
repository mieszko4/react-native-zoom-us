package ch.milosz.reactnative;

import android.content.Context;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.GroupViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import androidx.annotation.Nullable;
import java.util.Map;

public class RNZoomUsVideoViewManager extends GroupViewManager<RNZoomUsVideoView> {

  public static final String REACT_CLASS = "RNZoomUsVideoView";

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected RNZoomUsVideoView createViewInstance(ThemedReactContext themedReactContext) {
    return new RNZoomUsVideoView(themedReactContext);
  }

  @ReactProp(name="layout")
  public void setZoomLayout(RNZoomUsVideoView view, @Nullable ReadableArray layout) {
    view.setZoomLayout(layout);
  }
}
