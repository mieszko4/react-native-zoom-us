package ch.milosz.reactnative;

import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.media.projection.MediaProjectionManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Locale;

import us.zoom.sdk.InMeetingVideoController;
import us.zoom.sdk.InMeetingAudioController;
import us.zoom.sdk.InMeetingShareController;
import us.zoom.sdk.InMeetingUserInfo;
import us.zoom.sdk.MeetingEndReason;
import us.zoom.sdk.MeetingSettingsHelper;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKInitParams;

import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.InMeetingService;

import us.zoom.sdk.MobileRTCSDKError;

import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.MeetingOptions;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.JoinMeetingParams;

public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, LifecycleEventListener {

  private final static String TAG = "RNZoomUs";
  private final static int SCREEN_SHARE_REQUEST_CODE = 99;
  private final ReactApplicationContext reactContext;

  private RNZoomUsInMeetingServiceListener meetingListener;
  private Boolean shouldAutoConnectAudio = false;
  private Promise initializePromise;
  private Promise meetingPromise;

  private Boolean shouldDisablePreview = false;
  private Boolean customizedMeetingUIEnabled = false;

  private List<Integer> videoViews = Collections.synchronizedList(new ArrayList<Integer>());

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (requestCode == SCREEN_SHARE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
        startZoomScreenShare(intent);
      }
    }
  };

  public RNZoomUsModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
    reactContext.addActivityEventListener(mActivityEventListener);
  }

  @Override
  public String getName() {
    return "RNZoomUs";
  }

  @ReactMethod
  public void isInitialized(final Promise promise) {
    try {
      ZoomSDK zoomSDK = ZoomSDK.getInstance();

      Boolean isInitialized = zoomSDK.isInitialized();
      promise.resolve(isInitialized);
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void initialize(final ReadableMap params, final ReadableMap settings, final Promise promise) {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    if (zoomSDK.isInitialized()) {
      promise.resolve("Already initialize Zoom SDK successfully.");
      return;
    }
    initializePromise = promise;

    try {
      if (settings.hasKey("disableShowVideoPreviewWhenJoinMeeting")) {
        shouldDisablePreview = settings.getBoolean("disableShowVideoPreviewWhenJoinMeeting");
      }

      if (settings.hasKey("enableCustomizedMeetingUI")) {
        customizedMeetingUIEnabled = settings.getBoolean("enableCustomizedMeetingUI");
      }

      UiThreadUtil.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ZoomSDK zoomSDK = ZoomSDK.getInstance();

            String[] parts = settings.getString("language").split("-");
            Locale locale = parts.length == 1
              ? new Locale(parts[0])
              : new Locale(parts[0], parts[1]);
            zoomSDK.setSdkLocale(reactContext, locale);

            if (params.hasKey("jwtToken")) {
                ZoomSDKInitParams initParams = new ZoomSDKInitParams();
                initParams.jwtToken = params.getString("jwtToken");
                initParams.domain = params.getString("domain");
//              initParams.enableLog = true;
//              initParams.enableGenerateDump =true;
//              initParams.logSize = 5;

                zoomSDK.initialize(
                  reactContext.getCurrentActivity(),
                  RNZoomUsModule.this,
                  initParams
                );
            } else {
              ZoomSDKInitParams initParams = new ZoomSDKInitParams();
              initParams.appKey = params.getString("clientKey");
              initParams.appSecret = params.getString("clientSecret");
              initParams.domain = params.getString("domain");
              zoomSDK.initialize(getReactApplicationContext(), RNZoomUsModule.this, initParams);
            }
          }
      });
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void addVideoView(final int tagId, final Promise promise) {
    try {
      videoViews.add(new Integer(tagId));
      promise.resolve(null);
    } catch (Exception e) {
      promise.reject("ERR_ZOOM_VIDEO_VIEW", e.toString());
    }
  }

  @ReactMethod
  public void removeVideoView(final int tagId, final Promise promise) {
    try {
      videoViews.remove(new Integer(tagId));
      promise.resolve(null);
    } catch (Exception e) {
      promise.reject("ERR_ZOOM_VIDEO_VIEW", e.toString());
    }
  }

  @ReactMethod
  public void startMeeting(
    final ReadableMap paramMap,
    Promise promise
  ) {
    meetingPromise = promise;

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if(!zoomSDK.isInitialized()) {
            meetingPromise.reject("ERR_ZOOM_START", "ZoomSDK has not been initialized successfully");
            return;
          }

          final String meetingNo = paramMap.getString("meetingNumber");
          final MeetingService meetingService = zoomSDK.getMeetingService();
          if(meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
            long lMeetingNo = 0;
            try {
              lMeetingNo = Long.parseLong(meetingNo);
            } catch (NumberFormatException e) {
              meetingPromise.reject("ERR_ZOOM_START", "Invalid meeting number: " + meetingNo);
              return;
            }

            if(meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
              meetingService.returnToMeeting(reactContext.getCurrentActivity());
              meetingPromise.resolve("Already joined zoom meeting");
              return;
            }
          }

          StartMeetingOptions opts = new StartMeetingOptions();
          MeetingViewsOptions view = new MeetingViewsOptions();


          if(paramMap.hasKey("noInvite")) opts.no_invite = paramMap.getBoolean("noInvite");

          if(paramMap.hasKey("noButtonLeave") && paramMap.getBoolean("noButtonLeave")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_LEAVE;
          if(paramMap.hasKey("noButtonMore") && paramMap.getBoolean("noButtonMore")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_MORE;
          if(paramMap.hasKey("noButtonParticipants") && paramMap.getBoolean("noButtonParticipants")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_PARTICIPANTS;
          if(paramMap.hasKey("noButtonShare") && paramMap.getBoolean("noButtonShare")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_SHARE;
          if(paramMap.hasKey("noTextMeetingId") && paramMap.getBoolean("noTextMeetingId")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_MEETING_ID;
          if(paramMap.hasKey("noTextPassword") && paramMap.getBoolean("noTextPassword")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_PASSWORD;
          if(paramMap.hasKey("noShare")) opts.no_share = paramMap.getBoolean("noShare");

          StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
          params.displayName = paramMap.getString("userName");
          params.meetingNo = paramMap.getString("meetingNumber");
          params.userId = paramMap.getString("userId");
          params.userType = paramMap.getInt("userType");
          params.zoomAccessToken = paramMap.getString("zoomAccessToken");

          int startMeetingResult = meetingService.startMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
          Log.i(TAG, "startMeeting, startMeetingResult=" + startMeetingResult);

          if (startMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
            meetingPromise.reject("ERR_ZOOM_START", "startMeeting, errorCode=" + startMeetingResult);
          }
        } catch (Exception ex) {
          meetingPromise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void joinMeeting(
    final ReadableMap paramMap,
    Promise promise
  ) {
    meetingPromise = promise;
    shouldAutoConnectAudio = paramMap.getBoolean("autoConnectAudio");

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if(!zoomSDK.isInitialized()) {
            meetingPromise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
            return;
          }

          final MeetingService meetingService = zoomSDK.getMeetingService();

          JoinMeetingOptions opts = new JoinMeetingOptions();
          MeetingViewsOptions view = new MeetingViewsOptions();
          if(paramMap.hasKey("noAudio")) opts.no_audio = paramMap.getBoolean("noAudio");
          /**
              participant_id was removed from android options.
              There is no propper documentations and it still exists in jave docs...
              Maybe it was renamed to customer_key or so on. (todo check)
              Waiting before further changes.
           */
//          if(paramMap.hasKey("participantID")) opts.participant_id = paramMap.getString("participantID");

          if(paramMap.hasKey("noVideo")) opts.no_video = paramMap.getBoolean("noVideo");
          if(paramMap.hasKey("noInvite")) opts.no_invite = paramMap.getBoolean("noInvite");
          if(paramMap.hasKey("noBottomToolbar")) opts.no_bottom_toolbar = paramMap.getBoolean("noBottomToolbar");
          if(paramMap.hasKey("noPhoneDialIn")) opts.no_dial_in_via_phone = paramMap.getBoolean("noPhoneDialIn");
          if(paramMap.hasKey("noPhoneDialOut")) opts.no_dial_out_to_phone = paramMap.getBoolean("noPhoneDialOut");
          if(paramMap.hasKey("noMeetingEndMessage")) opts.no_meeting_end_message = paramMap.getBoolean("noMeetingEndMessage");
          if(paramMap.hasKey("noMeetingErrorMessage")) opts.no_meeting_error_message = paramMap.getBoolean("noMeetingErrorMessage");
          if(paramMap.hasKey("noShare")) opts.no_share = paramMap.getBoolean("noShare");
          if(paramMap.hasKey("noTitlebar")) opts.no_titlebar = paramMap.getBoolean("noTitlebar");
          if(paramMap.hasKey("customMeetingId")) opts.custom_meeting_id = paramMap.getString("customMeetingId");
          if(paramMap.hasKey("noDrivingMode")) opts.no_driving_mode = paramMap.getBoolean("noDrivingMode");
          if(paramMap.hasKey("noDisconnectAudio")) opts.no_disconnect_audio = paramMap.getBoolean("noDisconnectAudio");
          if(paramMap.hasKey("noRecord")) opts.no_record = paramMap.getBoolean("noRecord");
          if(paramMap.hasKey("noUnmuteConfirmDialog")) opts.no_unmute_confirm_dialog = paramMap.getBoolean("noUnmuteConfirmDialog");
          if(paramMap.hasKey("noWebinarRegisterDialog")) opts.no_webinar_register_dialog = paramMap.getBoolean("noWebinarRegisterDialog");
          if(paramMap.hasKey("noChatMsgToast")) opts.no_chat_msg_toast = paramMap.getBoolean("noChatMsgToast");


          /** TODO: posible extra options:
            opts.meeting_views_options = meetingOptions.meeting_views_options;
            opts.invite_options = meetingOptions.invite_options;
            opts.customer_key = meetingOptions.customer_key;
          */

          if(paramMap.hasKey("noButtonLeave") && paramMap.getBoolean("noButtonLeave")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_LEAVE;
          if(paramMap.hasKey("noButtonMore") && paramMap.getBoolean("noButtonMore")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_MORE;
          if(paramMap.hasKey("noButtonParticipants") && paramMap.getBoolean("noButtonParticipants")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_PARTICIPANTS;
          if(paramMap.hasKey("noButtonShare") && paramMap.getBoolean("noButtonShare")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_SHARE;
          if(paramMap.hasKey("noTextMeetingId") && paramMap.getBoolean("noTextMeetingId")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_MEETING_ID;
          if(paramMap.hasKey("noTextPassword") && paramMap.getBoolean("noTextPassword")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_PASSWORD;

          JoinMeetingParams params = new JoinMeetingParams();
          params.displayName = paramMap.getString("userName");
          params.meetingNo = paramMap.getString("meetingNumber");
          if(paramMap.hasKey("password")) params.password = paramMap.getString("password");
          if(paramMap.hasKey("webinarToken")) params.webinarToken = paramMap.getString("webinarToken");

          int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
          Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

          if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
            meetingPromise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
          }
        } catch (Exception ex) {
          meetingPromise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void leaveMeeting() {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        zoomSDK.getMeetingService().leaveCurrentMeeting(false);
      }
    });
  }

  @ReactMethod
  public void connectAudio(Promise promise) {
    connectAudioWithVoIP();
    promise.resolve(null);
  }

  @ReactMethod
  public void isMeetingConnected(final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.resolve(false);
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        promise.resolve(zoomSDK.getInMeetingService().isMeetingConnected());
      }
    });
  }

  @ReactMethod
  public void isMeetingHost(final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        promise.resolve(zoomSDK.getInMeetingService().isMeetingHost());
      }
    });
  }

  @ReactMethod
  public void getInMeetingUserIdList(final Promise promise) {
    final WritableArray rnUserList = Arguments.createArray();
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.resolve(rnUserList);
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final List<Long> userList = zoomSDK.getInMeetingService().getInMeetingUserList();

        for (final Long userId : userList) {
          rnUserList.pushString(userId.toString());
        }

        promise.resolve(rnUserList);
      }
    });
  }

  @ReactMethod
  public void muteMyVideo(final boolean muted, final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingVideoController videoController = zoomSDK.getInMeetingService().getInMeetingVideoController();

        MobileRTCSDKError result = videoController.muteMyVideo(muted);

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute my video error, status: " + result.name());
        }
      }
    });
  }

  @ReactMethod
  public void rotateMyVideo(final int rotation, final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingVideoController videoController = zoomSDK.getInMeetingService().getInMeetingVideoController();

        if (videoController.rotateMyVideo(rotation)) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Error: Rotate video failed");
        }
      }
    });
  }

  @ReactMethod
  public void muteMyAudio(final boolean muted, final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();

        MobileRTCSDKError result = audioController.muteMyAudio(muted);

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute my audio error, status: " + result.name());
        }
      }
    });
  }

  @ReactMethod
  public void muteAttendee(final String userId, final boolean muted, final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();

        MobileRTCSDKError result = audioController.muteAttendeeAudio(muted, Long.parseLong(userId));

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute attendee audio error, status: " + result.name());
        }
      }
    });
  }

  @ReactMethod
  public void muteAllAttendee(final boolean allowUnmuteSelf, final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();

        MobileRTCSDKError result = audioController.muteAllAttendeeAudio(allowUnmuteSelf);

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute all error, status: " + result.name());
        }
      }
    });
  }

  @ReactMethod
  public void startShareScreen(final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    if (customizedMeetingUIEnabled) {
      final MediaProjectionManager manager =
        (MediaProjectionManager) getReactApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

      if (manager != null) {
        Intent intent = manager.createScreenCaptureIntent();

        reactContext.getCurrentActivity().startActivityForResult(intent, SCREEN_SHARE_REQUEST_CODE);
      }

      promise.resolve(null);
    } else {
      final InMeetingShareController shareController = zoomSDK.getInMeetingService().getInMeetingShareController();

      MobileRTCSDKError result = shareController.startShareScreenContent();

      if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
        promise.resolve(null);
      } else {
        promise.reject("ERR_ZOOM_MEETING_CONTROL", "Start share screen error, status: " + result.name());
      }
    }
  }

  private void startZoomScreenShare(final Intent intent) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final ZoomSDK zoomSDK = ZoomSDK.getInstance();
        final InMeetingShareController shareController = zoomSDK.getInMeetingService().getInMeetingShareController();

        MobileRTCSDKError result = shareController.startShareScreenSession(intent);

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          // TODO: We should return the promise resolve
        } else {
          // TODO: We should return the promise rejection
        }
      }
    });
  }

  @ReactMethod
  public void stopShareScreen(final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingShareController shareController = zoomSDK.getInMeetingService().getInMeetingShareController();

        MobileRTCSDKError result = shareController.stopShareScreen();

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Stop share screen error, status: " + result.name());
        }
      }
    });
  }

  @ReactMethod
  public void switchCamera(final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingVideoController videoController = zoomSDK.getInMeetingService().getInMeetingVideoController();

        if (!videoController.isMyVideoMuted()) {
          if (videoController.switchToNextCamera()) {
            updateVideoView();
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Switch camera failed");
          }
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "The camera is muted");
        }
      }
    });
  }

  @ReactMethod
  public void raiseMyHand(final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        MobileRTCSDKError result = zoomSDK.getInMeetingService().raiseMyHand();

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Raise hand error, status: " + result.name());
        }
      }
    });
  }

  @ReactMethod
  public void lowerMyHand(final Promise promise) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

        MobileRTCSDKError result = inMeetingService.lowerHand(inMeetingService.getMyUserID());

        if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
          promise.resolve(null);
        } else {
          promise.reject("ERR_ZOOM_MEETING_CONTROL", "Lower hand error, status: " + result.name());
        }
      }
    });
  }

  @ReactMethod
  public void addListener(String eventName) {
      // Keep: Required for RN built in Event Emitter Calls.
  }
  @ReactMethod
  public void removeListeners(Integer count) {
      // Keep: Required for RN built in Event Emitter Calls.
  }

  // Internal user list update trigger
  public void updateVideoView() {
    UIManagerModule uiManager = reactContext.getNativeModule(UIManagerModule.class);

    uiManager.addUIBlock(new UIBlock() {
        @Override
        public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
          synchronized (videoViews) {
            Iterator<Integer> iterator = videoViews.iterator();
            while (iterator.hasNext()) {
              final int tagId = iterator.next();
              try {
                final RNZoomUsVideoView view = (RNZoomUsVideoView) nativeViewHierarchyManager.resolveView(tagId);
                if (view != null) view.update();
              } catch (Exception e) {
                Log.e(TAG, e.getMessage());
              }
            }
          }
        }
    });
  }

  @Override
  public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
    Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
    sendEvent("AuthEvent", getAuthErrorName(errorCode));
    if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
      initializePromise.reject(
              "ERR_ZOOM_INITIALIZATION",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
    } else {
      registerListener();
      initializePromise.resolve("Initialize Zoom SDK successfully.");

      final MeetingSettingsHelper meetingSettingsHelper = ZoomSDK.getInstance().getMeetingSettingsHelper();
      if (meetingSettingsHelper != null) {
        meetingSettingsHelper.disableShowVideoPreviewWhenJoinMeeting(shouldDisablePreview);
        meetingSettingsHelper.setCustomizedMeetingUIEnabled(customizedMeetingUIEnabled);
      }
    }
  }

  @Override
  public void onZoomAuthIdentityExpired(){}

  public void connectAudioWithVoIP() {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return;
    }

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();

        audioController.connectAudioWithVoIP();
      }
    });
  }

  private void registerListener() {
    Log.i(TAG, "registerListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    MeetingService meetingService = zoomSDK.getMeetingService();

    if(meetingService != null) {
      meetingService.addListener(this);
    }

    InMeetingService inMeetingService = zoomSDK.getInMeetingService();

    if (inMeetingService != null) {
      this.meetingListener = new RNZoomUsInMeetingServiceListener(reactContext, inMeetingService, this);

      inMeetingService.addListener(this.meetingListener);
      InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();

      if (inMeetingShareController != null) {
        inMeetingShareController.addListener(this.meetingListener);
      }
    }
  }

  private void unregisterListener() {
    Log.i(TAG, "unregisterListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(zoomSDK.isInitialized()) {
      final MeetingService meetingService = zoomSDK.getMeetingService();

      if (meetingService != null) {
        meetingService.removeListener(this);
      }

      final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

      if (inMeetingService != null) {
        inMeetingService.removeListener(this.meetingListener);
        final InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();

        if (inMeetingShareController != null) {
          inMeetingShareController.removeListener(this.meetingListener);
        }
      }
    }
  }

  @Override
  public void onCatalystInstanceDestroy() {
    unregisterListener();
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

  // React Native event emitters and event handling
  private void sendEvent(String name, String event) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(name, params);
  }

  private String getAuthErrorName(final int errorCode) {
    switch (errorCode) {
      case ZoomError.ZOOM_ERROR_AUTHRET_CLIENT_INCOMPATIBLEE: return "clientIncompatible";
      case ZoomError.ZOOM_ERROR_SUCCESS: return "success";
      case ZoomError.ZOOM_ERROR_DEVICE_NOT_SUPPORTED: return "deviceNotSupported"; // Android only
      case ZoomError.ZOOM_ERROR_ILLEGAL_APP_KEY_OR_SECRET: return "illegalAppKeyOrSecret"; // Android only
      case ZoomError.ZOOM_ERROR_INVALID_ARGUMENTS: return "invalidArguments"; // Android only
      case ZoomError.ZOOM_ERROR_NETWORK_UNAVAILABLE: return "networkUnavailable"; // Android only
      default: return "unknown";
    }
  }
}
