package ch.milosz.reactnative;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.List;

import us.zoom.sdk.InMeetingShareController.InMeetingShareListener;
import us.zoom.sdk.InMeetingServiceListener.RecordingStatus;
import us.zoom.sdk.InMeetingServiceListener.AudioStatus;
import us.zoom.sdk.InMeetingServiceListener.VideoStatus;
import us.zoom.sdk.FreeMeetingNeedUpgradeType;
import us.zoom.sdk.InMeetingShareController;
import us.zoom.sdk.InMeetingAudioController;
import us.zoom.sdk.InMeetingVideoController;
import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.InMeetingEventHandler;
import us.zoom.sdk.InMeetingChatMessage;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.SharingStatus;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.ShareSettingType;
import us.zoom.sdk.MeetingServiceListener;

public class RNZoomUsInMeetingServiceListener implements MeetingServiceListener, InMeetingServiceListener, InMeetingShareListener {
  private final static String EVENT_ID = "ZoomUsMeetingEvent";
  private final static String TAG = "RNZoomUsInMeetingServiceListener";
  private final ReactApplicationContext reactContext;
  private final InMeetingService inMeetingService;
  private final RNZoomUsModule module;

  public RNZoomUsInMeetingServiceListener(ReactApplicationContext context, InMeetingService inMeetingService, RNZoomUsModule module) {
    this.module = module;
    this.reactContext = context;
    this.inMeetingService = inMeetingService;
  }

  /*
   * Audio event listeners
   */
  @Override
  public void onHostAskUnMute(long userId) {
    sendEvent("audio.host.unmute", userId);
  }

  @Override
  public void onMyAudioSourceTypeChanged(int type) {
    WritableMap data = Arguments.createMap();
    data.putInt("type", type);

    sendEvent("audio.self.type.changed", data);
  }

  @Override
  public void onUserAudioStatusChanged(long userId, AudioStatus audioStatus) {
    sendEvent("audio.status.changed", userId, audioStatus);
  }

  @Override
  public void onMicrophoneStatusError(InMeetingAudioController.MobileRTCMicrophoneError error) {
    sendEvent("audio.status.error", error);
  }

  @Override
  public void onUserAudioTypeChanged(long userId) {
    sendEvent("audio.type.changed", userId);
  }

  @Override
  public void onSilentModeChanged(boolean inSilentMode) {
    WritableMap data = Arguments.createMap();
    data.putBoolean("inSilentMode", inSilentMode);

    sendEvent("audio.silentMode.changed", data);
  }

  /*
   * Video event listeners
   */
  @Override
  public void onHostAskStartVideo(long userId) {
    sendEvent("video.host.unmute", userId);
  }

  @Override
  public void onUserVideoStatusChanged(long userId, VideoStatus videoStatus) {
    sendEvent("video.host.unmute", userId, videoStatus);
  }

  @Override
  public void onActiveVideoUserChanged(long userId) {
    sendEvent("video.activeVideo.changed", userId);
  }

  @Override
  public void onMeetingActiveVideo(long userId) {
    sendEvent("video.active.changed", userId);
  }

  @Override
  public void onActiveSpeakerVideoUserChanged(long userId) {
    sendEvent("video.activeSpeaker.changed", userId);
  }

  @Override
  public void onSpotlightVideoChanged(boolean isSpotlightActive) {
    WritableMap data = Arguments.createMap();
    data.putBoolean("inSilentMode", isSpotlightActive);

    sendEvent("video.spotlight.changed", data);
  }

  /*
   * Users event listeners
   */
  @Override
  public void onMeetingUserJoin(List<Long> userIdList) {
    this.module.updateVideoView();
    sendEvent("users.joined", userIdList);
  }

  @Override
  public void onMeetingUserLeave(List<Long> userIdList) {
    this.module.updateVideoView();
    sendEvent("users.left", userIdList);
  }

  @Override
  public void onMeetingUserUpdated(long userId) {
    this.module.updateVideoView();
    sendEvent("users.updated", userId);
  }

  @Override
  public void onUserNameChanged(long userId, String userName) {
    this.module.updateVideoView();

    WritableMap data = Arguments.createMap();
    data.putDouble("userId", userId);
    data.putString("userName", userName);

    sendEvent("users.username.changed", data);
  }

  /*
   * Meeting event listeners
   */
  @Override
  public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
    Log.i(TAG, "onMeetingStatusChanged, meetingStatus=" + meetingStatus + ", errorCode=" + errorCode + ", internalErrorCode=" + internalErrorCode);

    this.module.updateVideoView();

    sendEvent("meeting.status.changed", meetingStatus, errorCode);

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
        this.module.connectAudioWithVoIP();
      }
    }
  }

  @Override
  public void onMeetingLeaveComplete(long ret) {
    this.module.updateVideoView();

    WritableMap data = Arguments.createMap();
    data.putString("reason", getMeetingEndReasonName((int)ret));

    sendEvent("meeting.leave.completed", data);
  }

  @Override
  public void onMeetingHostChanged(long userId) {
    sendEvent("meeting.host.changed", userId);
  }

  @Override
  public void onMeetingCoHostChanged(long userId) {
    sendEvent("meeting.cohost.changed", userId);
  }

  /*
   * Screen share event listeners
   */
  @Override
  public void onSharingStatus(SharingStatus sharingStatus, long userId) {
    this.module.updateVideoView();

    if (inMeetingService.isMyself(userId)) {
      final InMeetingShareController shareController = this.inMeetingService.getInMeetingShareController();

      if (shareController.isSharingOut()) {
        if (shareController.isSharingScreen()) {
            shareController.startShareScreenContent();
        }
      }
    }

    sendEvent("screenShare.status.changed", userId, sharingStatus);
  }

  @Override
  public void onShareSettingTypeChanged(ShareSettingType type) {
    WritableMap data = Arguments.createMap();
    data.putString("settingType", ShareSettingType.valueOf(type));

    sendEvent("screenShare.settings.changed", data);
  }

  @Override
  public void onShareUserReceivingStatus(long userId) {
    sendEvent("screenShare.receiving.status.changed", userId);
  }

  /*
   * Recording event listeners
   */
  @Override
  public void onRecordingStatus(RecordingStatus recordingStatus) {
    sendEvent("recording.local.changed", recordingStatus);
  }

  @Override
  public void onLocalRecordingStatus(RecordingStatus recordingStatus) {
    sendEvent("recording.cloud.changed", recordingStatus);
  }

  /*
   * Raise hands event listeners
   */
  @Override
  public void onLowOrRaiseHandStatusChanged(long userId, boolean isHandRaised) {
    WritableMap data = Arguments.createMap();
    data.putDouble("userId", userId);
    data.putBoolean("isHandRaised", userId);

    sendEvent("raiseHand.status.changed", data);
  }

  /*
   * Network event listeners
   */
  @Override
  public void onUserNetworkQualityChanged(long userId) {
    // TODO: We should find a way to get network quality
    sendEvent("network.quality.changed", userId);
  }

  /*
   * Closed caption event listeners
   */
  @Override
  public void onClosedCaptionReceived(String message) {
    WritableMap data = Arguments.createMap();
    data.putString("message", message);

    sendEvent("closeCaption.message.received", data);
  }

  /*
   * Chat event listeners
   */
  @Override
  public void onChatMessageReceived(InMeetingChatMessage message) {
    WritableMap data = Arguments.createMap();
    data.putString("message", message);

    sendEvent("chat.message.received", message);
  }

  @Override
  public void onSinkAttendeeChatPriviledgeChanged(int privilege) {
    WritableMap data = Arguments.createMap();
    data.putString("privilege", privilege);

    sendEvent("chat.attendeePrivilege.changed", data);
  }

  @Override
  public void onSinkAllowAttendeeChatNotification(int privilege) {
    WritableMap data = Arguments.createMap();
    data.putString("privilege", privilege);

    sendEvent("chat.allowAttendee.changed", data);
  }


  // InMeetingServiceListener required listeners but unused for now
  @Override
  public void onMeetingNeedPasswordOrDisplayName(boolean needPassword, boolean needDisplayName, InMeetingEventHandler handler) {}
  @Override
  public void onWebinarNeedRegister(String registerUrl) {}
  @Override
  public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler handler) {}
  @Override
  public void onMeetingNeedColseOtherMeeting(InMeetingEventHandler handler) {}
  @Override
  public void onMeetingFail(int errorCode, int internalErrorCode) {}
  @Override
  public void onInvalidReclaimHostkey() {}
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

  /*
   * Helpers
   */
  private void sendEvent(String event, long userId) {
    WritableMap data = Arguments.createMap();
    data.putDouble("userId", userId);

    sendEvent(event, data);
  }

  private void sendEvent(String event, long userId, AudioStatus audioStatus) {
    WritableMap data = Arguments.createMap();
    data.putDouble("userId", userId);
    data.putInt("audioStatus", AudioStatus.valueOf(audioStatus));

    sendEvent(event, data);
  }

  private void sendEvent(String event, long userId, VideoStatus videoStatus) {
    WritableMap data = Arguments.createMap();
    data.putDouble("userId", userId);
    data.putInt("videoStatus", VideoStatus.valueOf(videoStatus));

    sendEvent(event, data);
  }

  private void sendEvent(String event, long userId, SharingStatus sharingStatus) {
    WritableMap data = Arguments.createMap();
    // https://marketplacefront.zoom.us/sdk/meeting/android/us/zoom/sdk/SharingStatus.html
    data.putInt("sharingStatus", SharingStatus.valueOf(sharingStatus));

    sendEvent(event, data);
  }

  private void sendEvent(String event, RecordingStatus recordingStatus) {
    WritableMap data = Arguments.createMap();
    data.putInt("recordingStatus", VideoStatus.valueOf(recordingStatus));

    sendEvent(event, data);
  }

  private void sendEvent(String event, List<Long> userList) {
    WritableMap data = Arguments.createMap();
    WritableArray users = Arguments.createArray();

    for (final Long userId : userList) {
      users.pushString(userId.toString());
    }

    data.putArray("userIdList", users);

    sendEvent(event, data);
  }

  private void sendEvent(String event, MeetingStatus status, int errorCode) {
    WritableMap data = Arguments.createMap();
    data.putString("status", status.name());
    data.putInt("errorCode", errorCode);
    data.putString("errorName", getMeetingErrorName(errorCode));

    sendEvent(event, data);
  }

  private void sendEvent(String event, WritableMap data) {
    WritableMap params = Arguments.createMap();
    params.putString("event", event);
    params.putMap("data", data);

    reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(EVENT_ID, params);
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

  private String getMeetingErrorName(final int errorCode) {
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
}
