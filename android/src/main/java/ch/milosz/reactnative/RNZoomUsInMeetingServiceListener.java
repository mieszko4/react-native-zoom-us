package ch.milosz.reactnative;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.List;

// import us.zoom.sdk.FreeMeetingNeedUpgradeType;
import us.zoom.sdk.InMeetingAudioController;
import us.zoom.sdk.InMeetingChatMessage;
import us.zoom.sdk.InMeetingEventHandler;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.InMeetingUserInfo;

public class RNZoomUsInMeetingServiceListener implements InMeetingServiceListener {
    private final static String TAG = "RNZoomUsInMeetingServiceListener";
    private final ReactApplicationContext reactContext;
    private final InMeetingService inMeetingService;

    public RNZoomUsInMeetingServiceListener(ReactApplicationContext context, InMeetingService inMeetingService) {
        reactContext = context;
        this.inMeetingService = inMeetingService;
    }

    private void notifyEvent(String event, WritableMap params) {
        WritableMap map = Arguments.createMap();
        map.putString("event", event);
        map.putMap("payload", params);
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("InMeetingEvent", map);
    }

    private WritableMap extractUserInfoFromUserId(long userId) {
        InMeetingUserInfo userInfo = inMeetingService.getUserInfoById(userId);
        WritableMap map = Arguments.createMap();
        map.putString("name", userInfo.getUserName());
        map.putString("userId", "" + userId);
        // map.putString("participantId", userInfo.getParticipantID());
        return map;
    }

    @Override
    public void onWebinarNeedRegister() {

    }

    @Override
    public void onMeetingNeedPasswordOrDisplayName(boolean b, boolean b1, InMeetingEventHandler inMeetingEventHandler) {

    }

    @Override
    public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler inMeetingEventHandler) {

    }

    @Override
    public void onMeetingNeedColseOtherMeeting(InMeetingEventHandler inMeetingEventHandler) {

    }

    @Override
    public void onMeetingFail(int i, int i1) {

    }

    @Override
    public void onMeetingLeaveComplete(long l) {
    }

    @Override
    public void onMeetingUserJoin(List<Long> list) {
        WritableMap map = Arguments.createMap();
        WritableArray userList = Arguments.createArray();
        for (long l: list) {
            userList.pushMap(extractUserInfoFromUserId(l));
        }
        map.putArray("userList", userList);
        notifyEvent("meeting.user.joined", map);
    }

    @Override
    public void onMeetingUserLeave(List<Long> list) {
        WritableMap map = Arguments.createMap();
        WritableArray userList = Arguments.createArray();
        for (long l: list) {
            userList.pushMap(extractUserInfoFromUserId(l));
        }
        map.putArray("userList", userList);
        notifyEvent("meeting.user.left", map);
    }

    @Override
    public void onMeetingUserUpdated(long l) {

    }

    @Override
    public void onMeetingHostChanged(long l) {

    }

    @Override
    public void onMeetingCoHostChanged(long l) {

    }

    @Override
    public void onActiveVideoUserChanged(long l) {
        notifyEvent("meeting.user.video.active", extractUserInfoFromUserId(l));
    }

    @Override
    public void onActiveSpeakerVideoUserChanged(long l) {
        notifyEvent("meeting.user.video.speaker", extractUserInfoFromUserId(l));
    }

    @Override
    public void onSpotlightVideoChanged(boolean b) {

    }


    @Override
    public void onUserNetworkQualityChanged(long l) {

    }

    @Override
    public void onMicrophoneStatusError(InMeetingAudioController.MobileRTCMicrophoneError mobileRTCMicrophoneError) {

    }

    @Override
    public void onUserAudioStatusChanged(long l) {
        InMeetingUserInfo userInfo = inMeetingService.getUserInfoById(l);
        WritableMap map = Arguments.createMap();
        map.putString("name", userInfo.getUserName());
        map.putString("userId", "" + userInfo.getUserId());
        map.putBoolean("muted", userInfo.getAudioStatus().isMuted());
        notifyEvent("meeting.user.audio.status", map);
    }

    @Override
    public void onHostAskUnMute(long l) {

    }

    @Override
    public void onHostAskStartVideo(long l) {

    }

    @Override
    public void onUserAudioTypeChanged(long l) {

    }

    @Override
    public void onMyAudioSourceTypeChanged(int i) {

    }

    @Override
    public void onLowOrRaiseHandStatusChanged(long l, boolean b) {

    }

    @Override
    public void onMeetingSecureKeyNotification(byte[] bytes) {

    }

    @Override
    public void onChatMessageReceived(InMeetingChatMessage inMeetingChatMessage) {

    }

    @Override
    public void onSilentModeChanged(boolean b) {

    }

    @Override
    public void onFreeMeetingReminder(boolean b, boolean b1, boolean b2) {

    }

    @Override
    public void onMeetingActiveVideo(long l) {
    }

    @Override
    public void onSinkAttendeeChatPriviledgeChanged(int i) {

    }

    @Override
    public void onSinkAllowAttendeeChatNotification(int i) {

    }

    @Override
    public void onUserNameChanged(long l, String s) {

    }

//    @Override
//    public void onUserVideoStatusChanged(long l, VideoStatus videoStatus) {
//        WritableMap params = extractUserInfoFromUserId(l);
//        String status;
//        switch (videoStatus) {
//            case Video_ON:
//                status = "on";
//                break;
//            case Video_OFF:
//                status = "off";
//                break;
//            case Video_Mute_ByHost:
//                status = "muteByHost";
//                break;
//            default:
//                status = "undefined";
//        }
//        params.putString("status", status);
//        notifyEvent("meeting.user.video.status", params);
//    }

    @Override
    public void onUserVideoStatusChanged(long l) {
        InMeetingUserInfo userInfo = inMeetingService.getUserInfoById(l);
        WritableMap map = Arguments.createMap();
        map.putString("name", userInfo.getUserName());
        map.putString("userId", "" + userInfo.getUserId());
        map.putBoolean("active", userInfo.getVideoStatus().isSending());
        notifyEvent("meeting.user.video.status", map);
    }
//
//    @Override
//    public void onUserAudioStatusChanged(long l, AudioStatus audioStatus) {
//        WritableMap params = extractUserInfoFromUserId(l);
//        String status;
//        switch (audioStatus) {
//            case Audio_None:
//                status = "none";
//                break;
//            case Audio_Muted:
//                status = "muted";
//                break;
//            case Audio_Muted_ByHost:
//                status = "muteByHost";
//                break;
//            case Audio_UnMuted:
//                status = "unmute";
//                break;
//            case Audio_UnMuted_ByHost:
//                status = "unmuteByHost";
//                break;
//            case Audio_MutedAll_ByHost:
//                status = "mutedAllByHost";
//                break;
//            case Audio_UnMutedAll_ByHost:
//                status = "unmutedAllByHost";
//                break;
//            default:
//                status = "undefined";
//        }
//        params.putString("status", status);
//        notifyEvent("meeting.user.audio.status", params);
//    }
//
//    @Override
//    public void onFreeMeetingNeedToUpgrade(FreeMeetingNeedUpgradeType freeMeetingNeedUpgradeType, String s) {
//
//    }
//
//    @Override
//    public void onFreeMeetingUpgradeToGiftFreeTrialStart() {
//
//    }
//
//    @Override
//    public void onFreeMeetingUpgradeToGiftFreeTrialStop() {
//
//    }
//
//    @Override
//    public void onFreeMeetingUpgradeToProMeeting() {
//
//    }
}
