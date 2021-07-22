package ch.milosz.reactnative;

import android.util.Log;
import android.content.Context;
import android.view.View;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.MobileRTCVideoUnitRenderInfo;
import us.zoom.sdk.MobileRTCVideoUnitAspectMode;
import us.zoom.sdk.MobileRTCVideoView;
import us.zoom.sdk.MobileRTCVideoViewManager;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.InMeetingService;

import java.util.Collections;
import java.util.List;

// TODO: Solve SurfaceView blank problem.

class RNZoomUsVideoView extends MobileRTCVideoView {

  private final static String TAG = "RNZoomUs";

  private ReadableArray currentLayout = null;

  public RNZoomUsVideoView(Context context) {
    super(context);
  }

  public void setZoomLayout(ReadableArray layout) {
    currentLayout = layout;
    update();
  }

  private List<Long> getUserIdList() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return Collections.emptyList();
    }

    final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

    if (inMeetingService == null) {
      return Collections.emptyList();
    }

    return inMeetingService.getInMeetingUserList();
  }

  private long getActiveUser() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return 0;
    }

    final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

    if (inMeetingService == null) {
      return 0;
    }

    return inMeetingService.activeShareUserID();
  }

  public void update() {
    if (currentLayout == null) {
      return;
    }
    MobileRTCVideoViewManager manager = getVideoViewManager();
    if (manager == null) {
      Log.e(TAG, "The video view is not initialized complately");
      return;
    }
    try {
      manager.removeAllVideoUnits();
      List<Long> users = getUserIdList();
      Log.d(TAG, "Trig video update");
      for (int i = currentLayout.size() - 1; i > 0; --i) {
        ReadableMap unit = currentLayout.getMap(i);
        String kind = unit.hasKey("kind") ? unit.getString("kind") : "active";
        int x = unit.hasKey("x") ? unit.getInt("x") : 0;
        int y = unit.hasKey("y") ? unit.getInt("y") : 0;
        int width = unit.hasKey("width") ? unit.getInt("width") : 100;
        int height = unit.hasKey("height") ? unit.getInt("height") : 100;
        boolean border = unit.hasKey("border") ? unit.getBoolean("border") : false;
        boolean showUsername = unit.hasKey("showUsername") ? unit.getBoolean("showUsername") : true;
        boolean showAudioOff = unit.hasKey("showAudioOff") ? unit.getBoolean("showAudioOff") : true;
        int userIndex = unit.hasKey("userIndex") ? unit.getInt("userIndex") : 0;
        int background = unit.hasKey("background") ? unit.getInt("background") : 0x000000;
        MobileRTCVideoUnitRenderInfo renderInfo = new MobileRTCVideoUnitRenderInfo(x, y, width, height);
        if (border) {
          renderInfo.is_border_visible = border;
        }
        if (showUsername) {
          renderInfo.is_username_visible = showUsername;
        }
        if (showAudioOff) {
          renderInfo.is_show_audio_off = showAudioOff;
        }
        renderInfo.backgroud_color = background;
        switch (kind) {
          case "active":
            Log.d(TAG, "T=active speaker");
            manager.addActiveVideoUnit(renderInfo);
            break;
          case "preview":
            Log.d(TAG, "T=self preview");
            manager.addPreviewVideoUnit(renderInfo);
            break;
          case "share":
            Log.d(TAG, "T=user share");
            if (userIndex >= users.size() || userIndex < 0) {
              Log.i(TAG, "Index over user count");
              break;
            }
            manager.addShareVideoUnit(users.get(userIndex), renderInfo);
            break;
          case "attendee":
            Log.d(TAG, "T=user");
            if (userIndex >= users.size() || userIndex < 0) {
              Log.i(TAG, "Index over user count");
              break;
            }
            manager.addAttendeeVideoUnit(users.get(userIndex), renderInfo);
            break;
          case "active-share":
            Log.d(TAG, "T=active speaker share");
            long userId = getActiveUser();
            if (userId != 0) {
              manager.addShareVideoUnit(userId, renderInfo);
            } else {
              Log.i(TAG, "No active user");
            }
            break;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
  }

}
