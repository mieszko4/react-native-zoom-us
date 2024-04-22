package ch.milosz.reactnative;

import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.media.projection.MediaProjectionManager;
import androidx.fragment.app.FragmentActivity;

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

import java.lang.Long;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Locale;

import us.zoom.sdk.MeetingParameter;
import us.zoom.sdk.InMeetingVideoController;
import us.zoom.sdk.InMeetingAudioController;
import us.zoom.sdk.InMeetingChatMessage;
import us.zoom.sdk.InMeetingEventHandler;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.InMeetingShareController;
import us.zoom.sdk.InMeetingUserInfo;
import us.zoom.sdk.MeetingEndReason;
import us.zoom.sdk.MeetingSettingsHelper;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.FreeMeetingNeedUpgradeType;
import us.zoom.sdk.ShareSettingType;
import us.zoom.sdk.IRequestLocalRecordingPrivilegeHandler;
import us.zoom.sdk.LocalRecordingRequestPrivilegeStatus;
import us.zoom.sdk.MobileRTCFocusModeShareType;

import us.zoom.sdk.SharingStatus;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.MeetingServiceListener;

import us.zoom.sdk.MobileRTCSDKError;

import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.MeetingOptions;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.JoinMeetingParam4WithoutLogin;


import us.zoom.sdk.VideoQuality;
import us.zoom.sdk.ChatMessageDeleteType;
import us.zoom.sdk.InMeetingChatController;

// Please note that SDK initialization and all API call must run in Main Thread.
// See https://marketplace.zoom.us/docs/sdk/native-sdks/android/mastering-zoom-sdk/sdk-initialization/
public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, InMeetingServiceListener, MeetingServiceListener, InMeetingShareController.InMeetingShareListener, LifecycleEventListener {

  private final static String TAG = "RNZoomUs";
  private final static int SCREEN_SHARE_REQUEST_CODE = 99;
  private final ReactApplicationContext reactContext;

  private Boolean shouldAutoConnectAudio;
  private Promise initializePromise;
  private Promise meetingPromise;

  private Boolean shouldDisablePreview = false;
  private Boolean customizedMeetingUIEnabled = false;
  private Boolean disableClearWebKitCache = false;

  private List<Integer> videoViews = Collections.synchronizedList(new ArrayList<Integer>());

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, final Intent intent) {
      if (requestCode == SCREEN_SHARE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
        UiThreadUtil.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            try {
              startZoomScreenShare(intent);
            } catch (Exception ex) {
              Log.e(TAG, ex.getMessage());
            }
          }
        });
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
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          ZoomSDK zoomSDK = ZoomSDK.getInstance();

          Boolean isInitialized = zoomSDK.isInitialized();
          promise.resolve(isInitialized);
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void initialize(final ReadableMap params, final ReadableMap settings, final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          Log.i(TAG, "initialize");

          if (settings.hasKey("disableShowVideoPreviewWhenJoinMeeting")) {
            shouldDisablePreview = settings.getBoolean("disableShowVideoPreviewWhenJoinMeeting");
          }

          if (settings.hasKey("enableCustomizedMeetingUI")) {
            customizedMeetingUIEnabled = settings.getBoolean("enableCustomizedMeetingUI");
          }

          if (settings.hasKey("disableClearWebKitCache")) {
            disableClearWebKitCache = settings.getBoolean("disableClearWebKitCache");
          }

          ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if (zoomSDK.isInitialized()) {
            // Apply fresh settings

            // This setting process wouldn't be working because meetingSettingsHelper is null at this time.
            final MeetingSettingsHelper meetingSettingsHelper = ZoomSDK.getInstance().getMeetingSettingsHelper();
            if (meetingSettingsHelper != null) {
              meetingSettingsHelper.disableShowVideoPreviewWhenJoinMeeting(shouldDisablePreview);
              meetingSettingsHelper.setCustomizedMeetingUIEnabled(customizedMeetingUIEnabled);
              meetingSettingsHelper.disableClearWebKitCache(disableClearWebKitCache);
            }

            promise.resolve("Already initialize Zoom SDK successfully.");
            return;
          }

          String[] parts = settings.getString("language").split("-");
          Locale locale = parts.length == 1
            ? new Locale(parts[0])
            : new Locale(parts[0], parts[1]);
          zoomSDK.setSdkLocale(reactContext, locale);

          ZoomSDKInitParams initParams = new ZoomSDKInitParams();
          initParams.jwtToken = params.getString("jwtToken");
          initParams.domain = params.getString("domain");
          // initParams.enableLog = true;
          // initParams.enableGenerateDump =true;
          // initParams.logSize = 5;

          // Save promise so that it can be resolved in onZoomSDKInitializeResult
          // after zoomSDK.initialize is called
          initializePromise = promise;
          zoomSDK.initialize(reactContext.getCurrentActivity(), RNZoomUsModule.this, initParams);
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
          initializePromise = null;
        }
      }
    });
  }

  @ReactMethod
  public void addVideoView(final int tagId, final Promise promise) {
    try {
      videoViews.add(new Integer(tagId));
      promise.resolve(null);
    } catch (Exception ex) {
      promise.reject("ERR_ZOOM_VIDEO_VIEW", ex.toString());
    }
  }

  @ReactMethod
  public void removeVideoView(final int tagId, final Promise promise) {
    try {
      videoViews.remove(new Integer(tagId));
      promise.resolve(null);
    } catch (Exception ex) {
      promise.reject("ERR_ZOOM_VIDEO_VIEW", ex.toString());
    }
  }

  @ReactMethod
  public void startMeeting(
    final ReadableMap paramMap,
    final Promise promise
  ) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if(!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_START", "ZoomSDK has not been initialized successfully");
            return;
          }

          final String meetingNo = paramMap.getString("meetingNumber");
          final MeetingService meetingService = zoomSDK.getMeetingService();
          if(meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
            long lMeetingNo = 0;
            try {
              lMeetingNo = Long.parseLong(meetingNo);
            } catch (NumberFormatException e) {
              promise.reject("ERR_ZOOM_START", "Invalid meeting number: " + meetingNo);
              return;
            }

            if(meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
              meetingService.returnToMeeting(reactContext.getCurrentActivity());
              promise.resolve("Already joined zoom meeting");
              return;
            }
          }

          StartMeetingOptions opts = new StartMeetingOptions();
          MeetingViewsOptions view = new MeetingViewsOptions();


          if(paramMap.hasKey("noInvite")) opts.no_invite = paramMap.getBoolean("noInvite");
          if(paramMap.hasKey("noShare")) opts.no_share = paramMap.getBoolean("noShare");
          if(paramMap.hasKey("noMeetingErrorMessage")) opts.no_meeting_error_message = paramMap.getBoolean("noMeetingErrorMessage");

          if(paramMap.hasKey("noButtonLeave") && paramMap.getBoolean("noButtonLeave")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_LEAVE;
          if(paramMap.hasKey("noButtonMore") && paramMap.getBoolean("noButtonMore")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_MORE;
          if(paramMap.hasKey("noButtonParticipants") && paramMap.getBoolean("noButtonParticipants")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_PARTICIPANTS;
          if(paramMap.hasKey("noButtonShare") && paramMap.getBoolean("noButtonShare")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_SHARE;
          if(paramMap.hasKey("noTextMeetingId") && paramMap.getBoolean("noTextMeetingId")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_MEETING_ID;
          if(paramMap.hasKey("noTextPassword") && paramMap.getBoolean("noTextPassword")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_PASSWORD;

          StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
          params.displayName = paramMap.getString("userName");
          params.meetingNo = paramMap.getString("meetingNumber");
          params.userType = paramMap.getInt("userType");
          params.zoomAccessToken = paramMap.getString("zoomAccessToken");

          // Save promise so that it can be resolved in onMeetingStatusChanged
          // after zoomSDK.startMeetingWithParams is called
          meetingPromise = promise;
          int startMeetingResult = meetingService.startMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
          Log.i(TAG, "startMeeting, startMeetingResult=" + startMeetingResult);

          if (startMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
            // We are resolving promise: (1) right away and (2) in onMeetingStatusChanged because in case of no success onMeetingStatusChanged will not be triggered
            // It is not clear from docs (https://marketplace.zoom.us/docs/sdk/native-sdks/android/mastering-zoom-sdk/start-join-meeting/api-user/start-meeting)
            meetingPromise.reject("ERR_ZOOM_START", "startMeeting, errorCode=" + startMeetingResult);
            meetingPromise = null;
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
          meetingPromise = null;
        }
      }
    });
  }

  @ReactMethod
  public void joinMeeting(
    final ReadableMap paramMap,
    final Promise promise
  ) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if(!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
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

          JoinMeetingParam4WithoutLogin params = new JoinMeetingParam4WithoutLogin();
          params.displayName = paramMap.getString("userName");
          params.meetingNo = paramMap.getString("meetingNumber");
          if (paramMap.hasKey("password")) params.password = paramMap.getString("password");
          if (paramMap.hasKey("webinarToken")) params.webinarToken = paramMap.getString("webinarToken");
          if (paramMap.hasKey("zoomAccessToken")) params.zoomAccessToken = paramMap.getString("zoomAccessToken");

          // Save promise and shouldAutoConnectAudio so that it can be resolved in onMeetingStatusChanged
          // after zoomSDK.joinMeetingWithParams is called
          meetingPromise = promise;
          shouldAutoConnectAudio = paramMap.getBoolean("autoConnectAudio");
          int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
          Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

          if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
            // We are resolving promise: (1) right away and (2) in onMeetingStatusChanged because in case of no success onMeetingStatusChanged will not be triggered
            // It is not clear from docs (https://marketplace.zoom.us/docs/sdk/native-sdks/android/mastering-zoom-sdk/start-join-meeting/join-meeting)
            meetingPromise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
            meetingPromise = null;
            shouldAutoConnectAudio = null;
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
          meetingPromise = null;
          shouldAutoConnectAudio = null;
        }
      }
    });
  }

  @ReactMethod
  public void leaveMeeting(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.resolve(null);
            return;
          }

          zoomSDK.getMeetingService().leaveCurrentMeeting(false);
          promise.resolve(null);
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void connectAudio(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          connectAudioWithVoIP();
          promise.resolve(null);
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void isMeetingConnected(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.resolve(false);
            return;
          }

          promise.resolve(zoomSDK.getInMeetingService().isMeetingConnected());
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void isMeetingHost(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          promise.resolve(zoomSDK.getInMeetingService().isMeetingHost());
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void getInMeetingUserIdList(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final WritableArray rnUserList = Arguments.createArray();
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.resolve(rnUserList);
            return;
          }

          final List<Long> userList = zoomSDK.getInMeetingService().getInMeetingUserList();

          for (final Long userId : userList) {
            rnUserList.pushString(userId.toString());
          }

          promise.resolve(rnUserList);
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void muteMyVideo(final boolean muted, final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          final InMeetingVideoController videoController = zoomSDK.getInMeetingService().getInMeetingVideoController();

          MobileRTCSDKError result = videoController.muteMyVideo(muted);

          if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute my video error, status: " + result.name());
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void rotateMyVideo(final int rotation, final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          final InMeetingVideoController videoController = zoomSDK.getInMeetingService().getInMeetingVideoController();

          if (videoController.rotateMyVideo(rotation)) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Error: Rotate video failed");
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void muteMyAudio(final boolean muted, final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();

          MobileRTCSDKError result = audioController.muteMyAudio(muted);

          if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute my audio error, status: " + result.name());
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void muteAttendee(final String userId, final boolean muted, final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();

          MobileRTCSDKError result = audioController.muteAttendeeAudio(muted, Long.parseLong(userId));

          if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute attendee audio error, status: " + result.name());
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void muteAllAttendee(final boolean allowUnmuteSelf, final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();

          MobileRTCSDKError result = audioController.muteAllAttendeeAudio(allowUnmuteSelf);

          if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Mute all error, status: " + result.name());
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void startShareScreen(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          if (customizedMeetingUIEnabled) {
            final MediaProjectionManager manager =
              (MediaProjectionManager) reactContext.getCurrentActivity().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

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
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  private void startZoomScreenShare(final Intent intent) {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();
    final InMeetingShareController shareController = zoomSDK.getInMeetingService().getInMeetingShareController();

    MobileRTCSDKError result = shareController.startShareScreenSession(intent);

    if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
      sendEvent("MeetingEvent", "screenShareSuccess");
    } else {
      sendEvent("MeetingEvent", "screenShareError", result);
    }
  }

  @ReactMethod
  public void stopShareScreen(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          final InMeetingShareController shareController = zoomSDK.getInMeetingService().getInMeetingShareController();

          MobileRTCSDKError result = shareController.stopShareScreen();

          if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Stop share screen error, status: " + result.name());
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void switchCamera(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

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
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void raiseMyHand(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          MobileRTCSDKError result = zoomSDK.getInMeetingService().raiseMyHand();

          if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Raise hand error, status: " + result.name());
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
        }
      }
    });
  }

  @ReactMethod
  public void lowerMyHand(final Promise promise) {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();

          if (!zoomSDK.isInitialized()) {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "ZoomSDK has not been initialized successfully");
            return;
          }

          final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

          MobileRTCSDKError result = inMeetingService.lowerHand(inMeetingService.getMyUserID());

          if (result == MobileRTCSDKError.SDKERR_SUCCESS) {
            promise.resolve(null);
          } else {
            promise.reject("ERR_ZOOM_MEETING_CONTROL", "Lower hand error, status: " + result.name());
          }
        } catch (Exception ex) {
          promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
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
  private void updateVideoView() {
    UIManagerModule uiManager = reactContext.getNativeModule(UIManagerModule.class);

    uiManager.addUIBlock(new UIBlock() {
        @Override
        public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
          synchronized (videoViews) {
            Log.i(TAG, "updateVideoView");
            Iterator<Integer> iterator = videoViews.iterator();
            while (iterator.hasNext()) {
              final int tagId = iterator.next();
              try {
                final RNZoomUsVideoView view = (RNZoomUsVideoView) nativeViewHierarchyManager.resolveView(tagId);
                if (view != null) view.update();
              } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
              }
            }
          }
        }
    });
  }

  @Override
  public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
    Log.i(TAG, "onZoomSDKInitializeResult, errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);
    String errorInfo = getAuthErrorName(errorCode);
    sendEvent("AuthEvent", errorInfo);
    if(errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
      String errorFormatted = String.format("Error= %d (%s)", errorCode, errorInfo);
      initializePromise.reject(
        "ERR_ZOOM_INITIALIZATION",
         errorFormatted + ", internalErrorCode=" + internalErrorCode
      );
      initializePromise = null;
    } else {
      registerListener();
      initializePromise.resolve("Initialize Zoom SDK successfully.");
      initializePromise = null;

      // This might be the right spot for setMeetingSettings process
      final MeetingSettingsHelper meetingSettingsHelper = ZoomSDK.getInstance().getMeetingSettingsHelper();
      if (meetingSettingsHelper != null) {
        meetingSettingsHelper.disableShowVideoPreviewWhenJoinMeeting(shouldDisablePreview);
        meetingSettingsHelper.setCustomizedMeetingUIEnabled(customizedMeetingUIEnabled);
        meetingSettingsHelper.disableClearWebKitCache(disableClearWebKitCache);
      }
    }
  }

  @Override
  public void onZoomAuthIdentityExpired() {}

  // MeetingServiceListener
  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

    updateVideoView();

    sendEvent("MeetingEvent", getMeetErrorName(errorCode), meetingStatus);
    sendEvent("MeetingStatus", meetingStatus.name());

    if (meetingPromise == null) {
      Log.i(TAG, "onMeetingStatusChanged, does not have meetingPromise");
      return;
    }

    if(meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
      meetingPromise.reject(
              "ERR_ZOOM_MEETING",
              "Error: " + errorCode + ", internalErrorCode=" + internalErrorCode
      );
      meetingPromise = null;

      shouldAutoConnectAudio = null;
    } else if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
      meetingPromise.resolve("Connected to zoom meeting");
      meetingPromise = null;

      if (shouldAutoConnectAudio != null && shouldAutoConnectAudio == true) {
        connectAudioWithVoIP();
      }
      shouldAutoConnectAudio = null;
    }
  }

  private void connectAudioWithVoIP() {
    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return;
    }

    final InMeetingAudioController audioController = zoomSDK.getInMeetingService().getInMeetingAudioController();
    audioController.connectAudioWithVoIP();
  }

  private void registerListener() {
    Log.i(TAG, "registerListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    MeetingService meetingService = zoomSDK.getMeetingService();
    if(meetingService != null) {
      Log.i(TAG, "registerListener, added listener for meetingService");
      meetingService.addListener(this);
    }
    InMeetingService inMeetingService = zoomSDK.getInMeetingService();
    if (inMeetingService != null) {
      Log.i(TAG, "registerListener, added listener for inMeetingService");
      inMeetingService.addListener(this);
      InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();
      if (inMeetingShareController != null) {
        Log.i(TAG, "registerListener, added listener for getInMeetingShareController");
        inMeetingShareController.addListener(this);
      }
    }
  }

  private void unregisterListener() {
    Log.i(TAG, "unregisterListener");
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    if(zoomSDK.isInitialized()) {
      final MeetingService meetingService = zoomSDK.getMeetingService();
      if (meetingService != null) {
        Log.i(TAG, "unregisterListener, removed listener from meetingService");
        meetingService.removeListener(this);
      }
      final InMeetingService inMeetingService = zoomSDK.getInMeetingService();
      if (inMeetingService != null) {
        Log.i(TAG, "unregisterListener, removed listener from inMeetingService");
        inMeetingService.removeListener(this);
        final InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();
        if (inMeetingShareController != null) {
          Log.i(TAG, "unregisterListener, removed listener from inMeetingShareController");
          inMeetingShareController.removeListener(this);
        }
      }
    }
  }

  // InMeetingServiceListener required listeners
  @Override
  public void onMeetingLeaveComplete(long ret) {
    updateVideoView();
    sendEvent("MeetingEvent", getMeetingEndReasonName((int)ret));
  }

  @Override
  public void onMeetingUserJoin(List<Long> userIdList) {
    updateVideoView();
    sendEvent("MeetingEvent", "userJoin", userIdList);
  }

  @Override
  public void onMeetingUserLeave(List<Long> userIdList) {
    updateVideoView();
    sendEvent("MeetingEvent", "userLeave", userIdList);
  }

  @Override
  public void onHostAskUnMute(long userId) {
    sendEvent("MeetingEvent", "askUnMuteAudio");
  }

  @Override
  public void onHostAskStartVideo(long userId) {
    sendEvent("MeetingEvent", "askUnMuteVideo");
  }

  @Override
  public void onMeetingHostChanged(long userId) {
    sendEvent("MeetingEvent", "hostChanged", userId);
  }

  @Override
  @Deprecated
  public void onMeetingCoHostChanged(long userId) {}
  @Override
  public void onMeetingCoHostChange(long userId, boolean isCoHost) {
    sendEvent("MeetingEvent", "coHostChanged", userId);
  }

  @Override
  public void onMyAudioSourceTypeChanged(int type) {
    final InMeetingUserInfo userInfo = ZoomSDK.getInstance().getInMeetingService().getMyUserInfo();

    sendEvent("MeetingEvent", "myAudioSourceTypeChanged", userInfo);
  }

  @Override
  public void onUserAudioStatusChanged(long userId, AudioStatus audioStatus) {
    InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();

    if (userId == inMeetingService.getMyUserID()) {
      final InMeetingUserInfo userInfo = inMeetingService.getMyUserInfo();

      sendEvent("MeetingEvent", "myAudioStatusChanged", userInfo);
    }
  }

  @Override
  public void onUserVideoStatusChanged(long userId, VideoStatus videoStatus) {
    InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();

    if (userId == inMeetingService.getMyUserID()) {
      final InMeetingUserInfo userInfo = inMeetingService.getMyUserInfo();

      sendEvent("MeetingEvent", "myVideoStatusChanged", userInfo);
    }
  }

  @Override
  public void onAICompanionActiveChangeNotice(boolean b) {}
  @Override
  public void onParticipantProfilePictureStatusChange(boolean b) {}
  @Override
  public void onCloudRecordingStorageFull(long l) {}
  @Override
  public void onVideoAlphaChannelStatusChanged(boolean isAlphaModeOn) {}
  @Override
  public void onAllowParticipantsRequestCloudRecording(boolean bAllow) {}
  @Override
  public void onFocusModeStateChanged(boolean on) {}
  @Override
  public void onFocusModeShareTypeChanged(MobileRTCFocusModeShareType shareType) {}
  @Override
  public void onUVCCameraStatusChange(String cameraId, UVCCameraStatus status) {}

  @Override
  public void onInMeetingUserAvatarPathUpdated(long userId) {}
  @Override
  public void onSuspendParticipantsActivities() {}
  @Override
  public void onAllowParticipantsStartVideoNotification(boolean allow) {}
  @Override
  public void onAllowParticipantsRenameNotification(boolean allow) {}
  @Override
  public void onAllowParticipantsUnmuteSelfNotification(boolean allow) {}
  @Override
  public void onAllowParticipantsShareWhiteBoardNotification(boolean allow) {}
  @Override
  public void onMeetingLockStatus(boolean isLock) {}
  // InMeetingServiceListener required listeners but unused for now

  @Override
  public void onFollowHostVideoOrderChanged(boolean bFollow) {}
  @Override
  public void onMeetingParameterNotification(MeetingParameter meetingParameter) {}
  @Override
  public void onHostVideoOrderUpdated(List<Long> orderList) {}
  @Override
  public void onMeetingNeedPasswordOrDisplayName(boolean needPassword, boolean needDisplayName, InMeetingEventHandler handler) {}
  @Override
  public void onWebinarNeedRegister(String registerUrl) {}
  @Override
  public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler handler) {}
  @Override
  public void onMeetingNeedCloseOtherMeeting(InMeetingEventHandler handler) {}
  @Override
  public void onMeetingFail(int errorCode, int internalErrorCode) {}
  @Override
  public void onMeetingUserUpdated(long userId) {}
  @Override
  public void onActiveVideoUserChanged(long userId) {}
  @Override
  public void onActiveSpeakerVideoUserChanged(long userId) {}
  @Override
  @Deprecated
  public void onSpotlightVideoChanged(boolean on) {}
  @Override
  public void onSpotlightVideoChanged(List<Long> userList) {}
  @Override
  public void onSinkPanelistChatPrivilegeChanged(InMeetingChatController.MobileRTCWebinarPanelistChatPrivilege privilege) {}
  @Override
  @Deprecated
  public void onUserNetworkQualityChanged(long userId) {};
  @Override
  public void onSinkMeetingVideoQualityChanged(VideoQuality videoQuality, long userId) {}
  @Override
  public void onMicrophoneStatusError(InMeetingAudioController.MobileRTCMicrophoneError error) {}
  @Override
  public void onUserAudioTypeChanged(long userId) {}
  @Override
  public void onLowOrRaiseHandStatusChanged(long userId, boolean isRaisedHand) {}
  @Override
  public void onChatMessageReceived(InMeetingChatMessage inMeetingChatMessage) {}
  @Override
  public void onSilentModeChanged(boolean inSilentMode) {}
  @Override
  public void onMeetingActiveVideo(long userId) {}
  @Override
  public void onSinkAttendeeChatPriviledgeChanged(int privilege) {}
  @Override
  public void onSinkAllowAttendeeChatNotification(int privilege) {}
  @Override
  @Deprecated
  public void onUserNameChanged(long userId, String name) {}
  @Override
  public void onUserNamesChanged(List<Long> userList) {}
  @Override
  public void onInvalidReclaimHostkey() {}
  @Override
  public void onRecordingStatus(RecordingStatus status) {}
  @Override
  public void onLocalRecordingStatus(long userId, RecordingStatus recordingStatus) {}
  @Override
  public void onClosedCaptionReceived(String message, long senderId) {}
  @Override
  public void onFreeMeetingReminder(boolean isHost, boolean canUpgrade, boolean isFirstGift) {}
  @Override
  public void onFreeMeetingUpgradeToProMeeting() {}
  @Override
  public void onFreeMeetingUpgradeToGiftFreeTrialStop() {}
  @Override
  public void onFreeMeetingUpgradeToGiftFreeTrialStart() {}
  @Override
  public void onFreeMeetingNeedToUpgrade(FreeMeetingNeedUpgradeType type, String gifUrl) {}
  @Override
  public void onLocalVideoOrderUpdated(List<Long> localOrderList) {}
  @Override
  public void onAllHandsLowered() {}
  @Override
  public void onPermissionRequested(String[] permissions) {}
  @Override
  public void onChatMsgDeleteNotification(String msgID, ChatMessageDeleteType deleteBy) {}
  @Override
  public void onShareMeetingChatStatusChanged(boolean start) {}
  @Override
  public void onLocalRecordingPrivilegeRequested(IRequestLocalRecordingPrivilegeHandler handler) {}
  @Override
  public void onRequestLocalRecordingPrivilegeChanged(LocalRecordingRequestPrivilegeStatus status) {}

  // InMeetingShareListener event listeners
  // DEPRECATED: onShareActiveUser is just kept for now for backwards compatibility of events
  @Override
  public void onShareActiveUser(long userId) {
    final InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();

    if (inMeetingService.isMyself(userId)) {
      sendEvent("MeetingEvent", "screenShareStarted");
    } else if (userId == 0) {
      sendEvent("MeetingEvent", "screenShareStopped");
    }
  }

  @Override
  public void onShareSettingTypeChanged(ShareSettingType type) {}

  @Override
  public void onShareUserReceivingStatus(long userId) {}

  @Override
  public void onSharingStatus(SharingStatus status, long userId) {
    updateVideoView();

    sendEvent("MeetingEvent", getSharingStatusEventName(status), userId);

    if (status.equals(SharingStatus.Sharing_Self_Send_Begin)) {
      final InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();
      final InMeetingShareController shareController = inMeetingService.getInMeetingShareController();

      if (shareController.isSharingOut()) {
        if (shareController.isSharingScreen()) {
            shareController.startShareScreenContent();
        }
      }
    }
  }

  // React LifeCycle
  @Override
  public void onHostDestroy() {
    Log.i(TAG, "onHostDestroy");
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if (zoomSDK.isInitialized()) {
            zoomSDK.getMeetingService().leaveCurrentMeeting(false);
          }

          unregisterListener();
        } catch (Exception ex) {
          Log.e(TAG, ex.getMessage());
        }
      }
    });
  }
  @Override
  public void onHostPause() {
    Log.i(TAG, "onHostPause");
  }
  @Override
  public void onHostResume() {
    Log.i(TAG, "onHostResume");

    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if(!zoomSDK.isInitialized()) {
            return;
          }

          final MeetingService meetingService = zoomSDK.getMeetingService();
          List<MeetingStatus> staleMeetingStatuses = new ArrayList<>(Arrays.asList(MeetingStatus.MEETING_STATUS_IDLE, MeetingStatus.MEETING_STATUS_DISCONNECTING));
          if(!staleMeetingStatuses.contains(meetingService.getMeetingStatus())) {
            Log.i(TAG, "onHostResume, returning to meeting");
            meetingService.returnToMeeting(reactContext.getCurrentActivity());
          }

          registerListener();
        } catch (Exception ex) {
          Log.e(TAG, ex.getMessage());
        }
      }
    });
  }
  @Override
  public void onCatalystInstanceDestroy() {
    Log.i(TAG, "onCatalystInstanceDestroy");
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        try {
          final ZoomSDK zoomSDK = ZoomSDK.getInstance();
          if (zoomSDK.isInitialized()) {
            zoomSDK.getMeetingService().leaveCurrentMeeting(false);
          }

          unregisterListener();
        } catch (Exception ex) {
          Log.e(TAG, ex.getMessage());
        }
      }
    });
  }

  // React Native event emitters and event handling
  private void sendEvent(String name, String event) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(name, params);
  }

  private void sendEvent(String name, String event, InMeetingUserInfo userInfo) {
    if (userInfo != null) {
      WritableMap params = Arguments.createMap();
          params.putString("event", event);
          params.putString("userRole", userInfo.getInMeetingUserRole().name());
          params.putDouble("audioType", userInfo.getAudioStatus().getAudioType());

          params.putBoolean("isTalking", userInfo.getAudioStatus().isTalking());
          params.putBoolean("isMutedAudio", userInfo.getAudioStatus().isMuted());
          params.putBoolean("isMutedVideo", !userInfo.getVideoStatus().isSending());

        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(name, params);
    }
  }

  private void sendEvent(String name, String event, MeetingStatus status) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);
    params.putString("status", status.name());

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(name, params);
  }

  private void sendEvent(String name, String event, long userId) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);
    params.putDouble("userId", userId);

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(name, params);
  }

  private void sendEvent(String name, String event, List<Long> userList) {
    WritableMap params = Arguments.createMap();
    WritableArray users = Arguments.createArray();

    for (final Long userId : userList) {
      users.pushString(userId.toString());
    }

    params.putString("event", event);
    params.putArray("userList", users);

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(name, params);
  }

  private void sendEvent(String name, String event, MobileRTCSDKError error) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);
    params.putString("error", error.name());

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(name, params);
  }

  private String getSharingStatusEventName(final SharingStatus status) {
    switch (status) {
      case Sharing_Self_Send_Begin: return "screenShareStartedBySelf";
      case Sharing_Self_Send_End: return "screenShareStoppedBySelf";
      case Sharing_Other_Share_Begin: return "screenShareStartedByUser";
      case Sharing_Other_Share_End: return "screenShareStoppedByUser";
      case Sharing_View_Other_Sharing: return "screenShareOtherSharing";
      case Sharing_Pause: return "screenSharePause";
      case Sharing_Resume: return "screenShareResume";
      default: return "screenShareStoppedByUser";
    }
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

  private String getMeetErrorName(final int errorCode) {
    switch (errorCode) {
      case MeetingError.MEETING_ERROR_INVALID_ARGUMENTS: return "invalidArguments";
      case MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE: return "meetingClientIncompatible";
      case MeetingError.MEETING_ERROR_LOCKED: return "meetingLocked";
      case MeetingError.MEETING_ERROR_MEETING_NOT_EXIST: return "meetingNotExist";
      case MeetingError.MEETING_ERROR_MEETING_OVER: return "meetingOver";
      case MeetingError.MEETING_ERROR_RESTRICTED: return "meetingRestricted";
      case MeetingError.MEETING_ERROR_RESTRICTED_JBH: return "meetingRestrictedJBH";
      case MeetingError.MEETING_ERROR_USER_FULL: return "meetingUserFull";
      case MeetingError.MEETING_ERROR_MMR_ERROR: return "mmrError";
      case MeetingError.MEETING_ERROR_NETWORK_ERROR: return "networkError";
      case MeetingError.MEETING_ERROR_NO_MMR: return "noMMR";
      case MeetingError.MEETING_ERROR_HOST_DENY_EMAIL_REGISTER_WEBINAR: return "registerWebinarDeniedEmail";
      case MeetingError.MEETING_ERROR_WEBINAR_ENFORCE_LOGIN: return "registerWebinarEnforceLogin";
      case MeetingError.MEETING_ERROR_REGISTER_WEBINAR_FULL: return "registerWebinarFull";
      case MeetingError.MEETING_ERROR_DISALLOW_HOST_RESGISTER_WEBINAR: return "registerWebinarHostRegister";
      case MeetingError.MEETING_ERROR_DISALLOW_PANELIST_REGISTER_WEBINAR: return "registerWebinarPanelistRegister";
      case MeetingError.MEETING_ERROR_REMOVED_BY_HOST: return "removedByHost";
      case MeetingError.MEETING_ERROR_SESSION_ERROR: return "sessionError";
      case MeetingError.MEETING_ERROR_SUCCESS: return "success";
      case MeetingError.MEETING_ERROR_EXIT_WHEN_WAITING_HOST_START: return "exitWhenWaitingHostStart"; // Android only
      case MeetingError.MEETING_ERROR_INCORRECT_MEETING_NUMBER: return "incorrectMeetingNumber"; // Android only
      case MeetingError.MEETING_ERROR_INVALID_STATUS: return "invalidStatus"; // Android only
      case MeetingError.MEETING_ERROR_NETWORK_UNAVAILABLE: return "networkUnavailable"; // Android only
      case MeetingError.MEETING_ERROR_TIMEOUT: return "timeout"; // Android only
      case MeetingError.MEETING_ERROR_WEB_SERVICE_FAILED: return "webServiceFailed"; // Android only
      default: return "unknown";
    }
  }

  private String getMeetingEndReasonName(final int reason) {
    switch (reason) {
      case MeetingEndReason.END_BY_HOST: return "endedByHost";
      case MeetingEndReason.END_BY_HOST_START_ANOTHERMEETING: return "endedByHostForAnotherMeeting";
      case MeetingEndReason.END_BY_SELF: return "endedBySelf";
      case MeetingEndReason.END_BY_SDK_CONNECTION_BROKEN: return "endedConnectBroken";
      case MeetingEndReason.END_FOR_FREEMEET_TIMEOUT: return "endedFreeMeetingTimeout";
      case MeetingEndReason.END_FOR_JBHTIMEOUT: return "endedJBHTimeout";
      case MeetingEndReason.KICK_BY_HOST: return "endedRemovedByHost";
      case MeetingEndReason.END_FOR_NOATEENDEE: return "endedNoAttendee"; // Android only
      default: return "endedUnknownReason";
    }
  }
}
