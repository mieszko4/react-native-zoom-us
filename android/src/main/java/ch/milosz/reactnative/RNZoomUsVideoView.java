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
  private Boolean isNewConfig = false;
  private List<Long> lastAttendeeUserList = null;
  private long lastActiveUser = -1;

  public RNZoomUsVideoView(Context context) {
    super(context);
  }

  public void setZoomLayout(ReadableArray layout) {
    Log.i(TAG, "set layout");
    currentLayout = layout;
    isNewConfig = true;
    update();
  }

  private List<Long> getAttendeeWithoutMe() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return Collections.emptyList();
    }

    final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

    if (inMeetingService == null) {
      return Collections.emptyList();
    }

    List<Long> users = inMeetingService.getInMeetingUserList();

    if (users != null) {
      users.remove(new Long(inMeetingService.getMyUserID()));
    }

    return users;
  }

  private long getActiveUser() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return -1;
    }

    final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

    if (inMeetingService == null) {
      return -1;
    }

    return inMeetingService.activeShareUserID();
  }

  public void update() {
    if (currentLayout == null) {
      Log.e(TAG, "The video view no layout");
      return;
    }
    MobileRTCVideoViewManager manager = getVideoViewManager();
    if (manager == null) {
      Log.e(TAG, "The video view is not initialized complately");
      return;
    }
    try {
      boolean reLyout = isNewConfig;
      long activeUserId = getActiveUser();
      List<Long> users = getAttendeeWithoutMe();
      isNewConfig = false;
      if (lastAttendeeUserList == null || !lastAttendeeUserList.equals(users)) {
        lastAttendeeUserList = users;
        reLyout = true;
      }
      if (activeUserId != lastActiveUser) {
        lastActiveUser = activeUserId;
        reLyout = true;
      }
      if (reLyout) {
        Log.i(TAG, "Re-layout all video unit");
        manager.removeAllVideoUnits();
      }
      for (int i = 0; i < currentLayout.size(); i++) {
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
        int aspectMode = unit.hasKey("aspectMode") ? unit.getInt("aspectMode") : 0;
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
        renderInfo.aspect_mode = aspectMode;
        renderInfo.backgroud_color = background;
        Log.i(TAG, "Layout #" + i + " [kind=" + kind + " x=" + x + " y=" + y + "]");
        switch (kind) {
          case "active":
            if (reLyout) {
              manager.addActiveVideoUnit(renderInfo);
            } else {
              manager.updateActiveVideoUnit(renderInfo);
            }
            break;
          case "preview":
            if (reLyout) {
              manager.addPreviewVideoUnit(renderInfo);
            }
            break;
          case "share":
            if (users == null || userIndex >= users.size() || userIndex < 0) {
              Log.i(TAG, "Index over user count, skip add");
              break;
            }
            if (reLyout) {
              manager.addShareVideoUnit(users.get(userIndex), renderInfo);
            } else {
              manager.updateShareVideoUnit(renderInfo);
            }
            break;
          case "attendee":
            if (users == null || userIndex >= users.size() || userIndex < 0) {
              Log.i(TAG, "Index over user count, skip add");
              break;
            }
            if (reLyout) {
              manager.addAttendeeVideoUnit(users.get(userIndex), renderInfo);
            } else {
              manager.updateAttendeeVideoUnit(users.get(userIndex), renderInfo);
            }
            break;
          case "active-share":
            if (reLyout) {
              manager.addShareVideoUnit(activeUserId, renderInfo);
            } else {
              manager.updateShareVideoUnit(renderInfo);
            }
            break;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
  }

}
