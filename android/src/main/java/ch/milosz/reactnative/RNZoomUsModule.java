package ch.milosz.reactnative;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.List;

import us.zoom.sdk.InMeetingAudioController;
import us.zoom.sdk.InMeetingChatMessage;
import us.zoom.sdk.InMeetingEventHandler;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.InMeetingShareController;
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
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.InMeetingAudioController;

import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;

import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.JoinMeetingParams;

public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, InMeetingServiceListener, MeetingServiceListener, InMeetingShareController.InMeetingShareListener, LifecycleEventListener {

  private final static String TAG = "RNZoomUs";
  private final ReactApplicationContext reactContext;

  private Boolean isInitialized = false;
  private Boolean shouldAutoConnectAudio = false;
  private Promise initializePromise;
  private Promise meetingPromise;

  private Boolean shouldDisablePreview = false;

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
  public void initialize(final ReadableMap params, final ReadableMap settings, final Promise promise) {
    if (isInitialized) {
      promise.resolve("Already initialize Zoom SDK successfully.");
      return;
    }

    isInitialized = true;

    try {
      initializePromise = promise;

      if (settings.hasKey("disableShowVideoPreviewWhenJoinMeeting")) {
        shouldDisablePreview = settings.getBoolean("disableShowVideoPreviewWhenJoinMeeting");
      }

      reactContext.getCurrentActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            ZoomSDK zoomSDK = ZoomSDK.getInstance();

            if (params.hasKey("jwtToken")) {
                ZoomSDKInitParams initParams = new ZoomSDKInitParams();
                initParams.jwtToken = params.getString("jwtToken");
                initParams.domain = params.getString("domain");

                zoomSDK.initialize(
                  reactContext.getCurrentActivity(),
                  RNZoomUsModule.this,
                  initParams
                );
            } else {
              zoomSDK.initialize(
                reactContext.getCurrentActivity(),
                params.getString("clientKey"),
                params.getString("clientSecret"),
                params.getString("domain"),
                RNZoomUsModule.this
              );
            }
          }
      });
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void startMeeting(
    final ReadableMap paramMap,
    Promise promise
  ) {
    try {
      meetingPromise = promise;

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

      if(paramMap.hasKey("noButtonLeave") && paramMap.getBoolean("noButtonLeave")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_LEAVE;
      if(paramMap.hasKey("noButtonMore") && paramMap.getBoolean("noButtonMore")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_MORE;
      if(paramMap.hasKey("noButtonParticipants") && paramMap.getBoolean("noButtonParticipants")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_PARTICIPANTS;
      if(paramMap.hasKey("noButtonShare") && paramMap.getBoolean("noButtonShare")) opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_SHARE;
      if(paramMap.hasKey("noTextMeetingId") && paramMap.getBoolean("noTextMeetingId")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_MEETING_ID;
      if(paramMap.hasKey("noTextPassword") && paramMap.getBoolean("noTextPassword")) opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_PASSWORD;

      StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
      params.displayName = paramMap.getString("userName");
      params.meetingNo = paramMap.getString("meetingNumber");
      params.userId = paramMap.getString("userId");
      params.userType = paramMap.getInt("userType");
      params.zoomAccessToken = paramMap.getString("zoomAccessToken");

      int startMeetingResult = meetingService.startMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "startMeeting, startMeetingResult=" + startMeetingResult);

      if (startMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_START", "startMeeting, errorCode=" + startMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void joinMeeting(
    final ReadableMap paramMap,
    Promise promise
  ) {
    try {
      meetingPromise = promise;
      shouldAutoConnectAudio = paramMap.getBoolean("autoConnectAudio");

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if(!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();

      JoinMeetingOptions opts = new JoinMeetingOptions();
      MeetingViewsOptions view = new MeetingViewsOptions();
      if(paramMap.hasKey("participantID")) opts.participant_id = paramMap.getString("participantID");
      if(paramMap.hasKey("noAudio")) opts.no_audio = paramMap.getBoolean("noAudio");
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

      int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

      if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void joinMeetingWithPassword(
    final String displayName,
    final String meetingNo,
    final String password,
    Promise promise
  ) {
    try {
      meetingPromise = promise;

      ZoomSDK zoomSDK = ZoomSDK.getInstance();
      if(!zoomSDK.isInitialized()) {
        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
        return;
      }

      final MeetingService meetingService = zoomSDK.getMeetingService();

      JoinMeetingOptions opts = new JoinMeetingOptions();
      JoinMeetingParams params = new JoinMeetingParams();
      params.displayName = displayName;
      params.meetingNo = meetingNo;
      params.password = password;

      int joinMeetingResult = meetingService.joinMeetingWithParams(reactContext.getCurrentActivity(), params, opts);
      Log.i(TAG, "joinMeeting, joinMeetingResult=" + joinMeetingResult);

      if (joinMeetingResult != MeetingError.MEETING_ERROR_SUCCESS) {
        promise.reject("ERR_ZOOM_JOIN", "joinMeeting, errorCode=" + joinMeetingResult);
      }
    } catch (Exception ex) {
      promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
    }
  }

  @ReactMethod
  public void leaveMeeting() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return;
    }

    final MeetingService meetingService = zoomSDK.getMeetingService();

    meetingService.leaveCurrentMeeting(false);
  }

  @ReactMethod
  public void connectAudio() {
    connectAudioWithVoIP();
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
      }
    }
  }

  @Override
  public void onZoomAuthIdentityExpired(){
  }

  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

    sendEvent("MeetingEvent", getMeetErrorName(errorCode), meetingStatus);

    if (meetingPromise == null) {
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

      if (shouldAutoConnectAudio == true) {
        connectAudioWithVoIP();
      }
    }
  }

  private void connectAudioWithVoIP() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if (!zoomSDK.isInitialized()) {
      return;
    }

    final InMeetingService inMeetingService = zoomSDK.getInMeetingService();

    final InMeetingAudioController audioController = inMeetingService.getInMeetingAudioController();

    audioController.connectAudioWithVoIP();
    audioController.muteMyAudio(false);
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
      inMeetingService.addListener(this);
      InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();
      if (inMeetingShareController != null) {
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
        meetingService.removeListener(this);
      }
      final InMeetingService inMeetingService = zoomSDK.getInMeetingService();
      if (inMeetingService != null) {
        inMeetingService.removeListener(this);
        final InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();
        if (inMeetingShareController != null) {
          inMeetingShareController.removeListener(this);
        }
      }
    }
  }

  @Override
  public void onShareActiveUser(long userId) {
    if (userId == ZoomSDK.getInstance().getInMeetingService().getMyUserID()) {
      sendEvent("MeetingEvent", "screenShareStarted");
    } else {
      sendEvent("MeetingEvent", "screenShareStopped");
    }
  }

  @Override
  public void onShareUserReceivingStatus(long l) {}


  @Override
  public void onCatalystInstanceDestroy() {
    unregisterListener();
  }

  @Override
  public void onMeetingLeaveComplete(long ret) {
    sendEvent("MeetingEvent", getMeetingEndReasonName((int)ret));
  }

  // Required methods for InMeetingServiceListener
  @Override
  public void onMeetingNeedPasswordOrDisplayName(boolean b, boolean b1, InMeetingEventHandler inMeetingEventHandler) {}
  @Override
  public void onWebinarNeedRegister() {}
  @Override
  public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler inMeetingEventHandler) {}
  @Override
  public void onMeetingNeedColseOtherMeeting(InMeetingEventHandler inMeetingEventHandler) {}
  @Override
  public void onMeetingFail(int i, int i1) {}
  @Override
  public void onMeetingUserJoin(List<Long> list) {}
  @Override
  public void onMeetingUserLeave(List<Long> list) {}
  @Override
  public void onMeetingUserUpdated(long l) {}
  @Override
  public void onMeetingHostChanged(long l) {}
  @Override
  public void onMeetingCoHostChanged(long l) {}
  @Override
  public void onActiveVideoUserChanged(long l) {}
  @Override
  public void onActiveSpeakerVideoUserChanged(long l) {}
  @Override
  public void onSpotlightVideoChanged(boolean b) {}
  @Override
  public void onUserVideoStatusChanged(long l) {}
  @Override
  public void onUserNetworkQualityChanged(long l) {}
  @Override
  public void onMicrophoneStatusError(InMeetingAudioController.MobileRTCMicrophoneError mobileRTCMicrophoneError) {}
  @Override
  public void onUserAudioStatusChanged(long l) {}
  @Override
  public void onHostAskUnMute(long l) {}
  @Override
  public void onHostAskStartVideo(long l) {}
  @Override
  public void onUserAudioTypeChanged(long l) {}
  @Override
  public void onMyAudioSourceTypeChanged(int i) {}
  @Override
  public void onLowOrRaiseHandStatusChanged(long l, boolean b) {}
  @Override
  public void onMeetingSecureKeyNotification(byte[] bytes) {}
  @Override
  public void onChatMessageReceived(InMeetingChatMessage inMeetingChatMessage) {}
  @Override
  public void onSilentModeChanged(boolean b) {}
  @Override
  public void onFreeMeetingReminder(boolean b, boolean b1, boolean b2) {}
  @Override
  public void onMeetingActiveVideo(long l) {}
  @Override
  public void onSinkAttendeeChatPriviledgeChanged(int i) {}
  @Override
  public void onSinkAllowAttendeeChatNotification(int i) {}
  @Override
  public void onUserNameChanged(long l, String s) {}

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
  private void sendEvent(String name, String event, MeetingStatus status) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);
    params.putString("status", status.name());

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
