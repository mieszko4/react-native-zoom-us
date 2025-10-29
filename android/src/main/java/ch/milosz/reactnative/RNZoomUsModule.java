package ch.milosz.reactnative;

import static us.zoom.sdk.ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_All;
import static us.zoom.sdk.ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_Individual;

import android.os.Handler;
import android.os.Looper;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Locale;

import us.zoom.sdk.BOControllerError;
import us.zoom.sdk.BOOption;
import us.zoom.sdk.BOStatus;
import us.zoom.sdk.CameraControlRequestResult;
import us.zoom.sdk.CameraControlRequestType;
import us.zoom.sdk.ChatMessageBuilder;
import us.zoom.sdk.IBOAdmin;
import us.zoom.sdk.IBOAdminEvent;
import us.zoom.sdk.IBOAssistant;
import us.zoom.sdk.IBOAttendee;
import us.zoom.sdk.IBOAttendeeEvent;
import us.zoom.sdk.IBOCreator;
import us.zoom.sdk.IBOData;
import us.zoom.sdk.ICameraControlRequestHandler;
import us.zoom.sdk.IMeetingArchiveConfirmHandler;
import us.zoom.sdk.IMeetingInputUserInfoHandler;
import us.zoom.sdk.IRecoverMeetingHandle;
import us.zoom.sdk.IShareAction;
import us.zoom.sdk.InMeetingBOController;
import us.zoom.sdk.InMeetingBOControllerListener;
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
import us.zoom.sdk.MobileRTCShareView;
import us.zoom.sdk.ReturnToMainSessionHandler;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDKChatMessageType;
import us.zoom.sdk.ZoomSDKFileReceiver;
import us.zoom.sdk.ZoomSDKFileSender;
import us.zoom.sdk.ZoomSDKFileTransferInfo;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.FreeMeetingNeedUpgradeType;
import us.zoom.sdk.ShareSettingType;
import us.zoom.sdk.IRequestLocalRecordingPrivilegeHandler;
import us.zoom.sdk.LocalRecordingRequestPrivilegeStatus;

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
import us.zoom.sdk.MobileRTCFocusModeShareType;
import us.zoom.sdk.ZoomSDKSharingSourceInfo;
import us.zoom.sdk.ZoomUIService;

// Please note that SDK initialization and all API call must run in Main Thread.
// See https://marketplace.zoom.us/docs/sdk/native-sdks/android/mastering-zoom-sdk/sdk-initialization/
public class RNZoomUsModule extends ReactContextBaseJavaModule implements ZoomSDKInitializeListener, InMeetingServiceListener, MeetingServiceListener, InMeetingShareController.InMeetingShareListener, LifecycleEventListener, InMeetingBOControllerListener {

    private final static String TAG = "RNZoomUs";
    private final static int SCREEN_SHARE_REQUEST_CODE = 99;
    private final ReactApplicationContext reactContext;

    private Boolean shouldAutoConnectAudio;
    private Promise initializePromise;
    private Promise meetingPromise;
    private InMeetingBOController mInMeetingBOController;

    private Boolean shouldDisablePreview = false;
    private Boolean customizedMeetingUIEnabled = false;
    private Boolean disableClearWebKitCache = false;

    private Boolean meetingShareHidden = false;
    private Boolean meetingVideoHidden = false;
    private Boolean meetingAudioHidden = false;
    private Boolean closeCaptionHidden = false;
    private Boolean disableCloudWhiteboard = false;
    private Boolean meetingMoreHidden = false;

    private static final int MAX_RETRY = 5;
    private int retryCount = 0;
    MobileRTCShareView shareView = new MobileRTCShareView(getReactApplicationContext());

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

                    if (settings.hasKey("meetingShareHidden")) {
                        meetingShareHidden = settings.getBoolean("meetingShareHidden");
                    }

                    if (settings.hasKey("meetingVideoHidden")) {
                        meetingVideoHidden = settings.getBoolean("meetingVideoHidden");
                    }

                    if (settings.hasKey("meetingAudioHidden")) {
                        meetingAudioHidden = settings.getBoolean("meetingAudioHidden");
                    }

                    if (settings.hasKey("closeCaptionHidden")) {
                        closeCaptionHidden = settings.getBoolean("closeCaptionHidden");
                    }

                    if (settings.hasKey("disableCloudWhiteboard")) {
                        disableCloudWhiteboard = settings.getBoolean("disableCloudWhiteboard");
                    }

                    if (settings.hasKey("meetingMoreHidden")) {
                        meetingMoreHidden = settings.getBoolean("meetingMoreHidden");
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
                            meetingSettingsHelper.setHideShareButtonInMeetingToolbar(meetingShareHidden);
                            meetingSettingsHelper.setClosedCaptionHidden(closeCaptionHidden);
                            meetingSettingsHelper.enableCloudWhiteboard(!disableCloudWhiteboard);
                            meetingSettingsHelper.hideCloudWhiteboardHelperCenterButton(disableCloudWhiteboard);
                            meetingSettingsHelper.hideCloudWhiteboardOpenInBrowserButton(disableCloudWhiteboard);
                        }

                        promise.resolve("Already initialize Zoom SDK successfully.");
                        return;
                    }

                    String[] parts = settings.getString("language").split("-");
                    Locale locale = parts.length == 1
                            ? new Locale(parts[0])
                            : new Locale(parts[0], parts[1]);
//                    zoomSDK.setSdkLocale(reactContext, Locale.ROOT);

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
    public void cleanup(final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZoomSDK zoomSDK = ZoomSDK.getInstance();
                    if (!zoomSDK.isInitialized()) {
                        promise.resolve("Zoom SDK is not initialized");
                        return;
                    }
                    zoomSDK.uninitialize();
                    promise.resolve("Already cleanup Zoom SDK successfully.");
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                    initializePromise = null;
                }
            }
        });
    }

    private final IBOAdminEvent iboAdminEvent = new IBOAdminEvent() {
        @Override
        public void onStartBOResponse(boolean bSuccess) {

        }

        @Override
        public void onStopBOResponse(boolean bSuccess) {

        }

        @Override
        public void onHelpRequestReceived(String strUserID) {

        }

        @Override
        public void onStartBOError(BOControllerError error) {
            String errorInfo = getStartBOErrorName(error);
            sendEvent("onStartBOError", errorInfo);
        }

        @Override
        public void onBOEndTimerUpdated(int remaining, boolean isTimesUpNotice) {

        }

        private String getStartBOErrorName(final BOControllerError errorCode) {
            return switch (errorCode) {
                case BOControllerError_BO_LIST_IS_UPLOADING -> "BO_LIST_IS_UPLOADING";
                case BOControllerError_NO_PRIVILEGE -> "NO_PRIVILEGE";
                case BOControllerError_NO_ONE_HAS_BEEN_ASSIGNED ->
                        "NO_ONE_HAS_BEEN_ASSIGNED"; // Android only
                case BOControllerError_NULL_POINTER -> "NULL_POINTER"; // Android only
                case BOControllerError_UNKNOWN -> "UNKNOWN"; // Android only
                case BOControllerError_TOKEN_NOT_READY -> "TOKEN_NOT_READY"; // Android only
                case BOControllerError_UPLOAD_FAIL -> "UPLOAD_FAIL"; // Android only
                case BOControllerError_WRONG_CURRENT_STATUS ->
                        "WRONG_CURRENT_STATUS"; // Android only
                default -> "UNKNOWN";
            };
        }
    };

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
                    if (!zoomSDK.isInitialized()) {
                        promise.reject("ERR_ZOOM_START", "ZoomSDK has not been initialized successfully");
                        return;
                    }

                    final String meetingNo = paramMap.getString("meetingNumber");
                    final MeetingService meetingService = zoomSDK.getMeetingService();
                    if (meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
                        long lMeetingNo = 0;
                        try {
                            lMeetingNo = Long.parseLong(meetingNo);
                        } catch (NumberFormatException e) {
                            promise.reject("ERR_ZOOM_START", "Invalid meeting number: " + meetingNo);
                            return;
                        }

                        if (meetingService.getCurrentRtcMeetingNumber() == lMeetingNo) {
                            meetingService.returnToMeeting(reactContext.getCurrentActivity());
                            promise.resolve("Already joined zoom meeting");
                            return;
                        }
                    }

                    StartMeetingOptions opts = new StartMeetingOptions();

                    if (paramMap.hasKey("noInvite"))
                        opts.no_invite = paramMap.getBoolean("noInvite");
                    if (paramMap.hasKey("noShare")) opts.no_share = paramMap.getBoolean("noShare");
                    if (paramMap.hasKey("noMeetingErrorMessage"))
                        opts.no_meeting_error_message = paramMap.getBoolean("noMeetingErrorMessage");

                    if (paramMap.hasKey("noButtonLeave") && paramMap.getBoolean("noButtonLeave"))
                        opts.meeting_views_options = opts.meeting_views_options + MeetingViewsOptions.NO_BUTTON_LEAVE;
                    if ((paramMap.hasKey("noButtonMore") && paramMap.getBoolean("noButtonMore")) || meetingMoreHidden)
                        opts.meeting_views_options = opts.meeting_views_options + MeetingViewsOptions.NO_BUTTON_MORE;
                    if (paramMap.hasKey("noButtonParticipants") && paramMap.getBoolean("noButtonParticipants"))
                        opts.meeting_views_options = opts.meeting_views_options + MeetingViewsOptions.NO_BUTTON_PARTICIPANTS;
                    if (paramMap.hasKey("noButtonShare") && paramMap.getBoolean("noButtonShare"))
                        opts.meeting_views_options = opts.meeting_views_options + MeetingViewsOptions.NO_BUTTON_SHARE;
                    if (paramMap.hasKey("noTextMeetingId") && paramMap.getBoolean("noTextMeetingId"))
                        opts.meeting_views_options = opts.meeting_views_options + MeetingViewsOptions.NO_TEXT_MEETING_ID;
                    if (paramMap.hasKey("noTextPassword") && paramMap.getBoolean("noTextPassword"))
                        opts.meeting_views_options = opts.meeting_views_options + MeetingViewsOptions.NO_TEXT_PASSWORD;
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
                    if (!zoomSDK.isInitialized()) {
                        promise.reject("ERR_ZOOM_JOIN", "ZoomSDK has not been initialized successfully");
                        return;
                    }

                    final MeetingService meetingService = zoomSDK.getMeetingService();

                    IBOAdmin iboAdmin = ZoomSDK.getInstance().getInMeetingService().getInMeetingBOController().getBOAdminHelper();
                    if (iboAdmin != null)
                        iboAdmin.setEvent(iboAdminEvent);

                    JoinMeetingOptions opts = new JoinMeetingOptions();
                    MeetingViewsOptions view = new MeetingViewsOptions();
                    if (paramMap.hasKey("noAudio")) opts.no_audio = paramMap.getBoolean("noAudio");
                    /**
                     participant_id was removed from android options.
                     There is no propper documentations and it still exists in jave docs...
                     Maybe it was renamed to customer_key or so on. (todo check)
                     Waiting before further changes.
                     */
//          if(paramMap.hasKey("participantID")) opts.participant_id = paramMap.getString("participantID");

                    if (paramMap.hasKey("noVideo")) opts.no_video = paramMap.getBoolean("noVideo");
                    if (paramMap.hasKey("noInvite"))
                        opts.no_invite = paramMap.getBoolean("noInvite");
                    if (paramMap.hasKey("noBottomToolbar"))
                        opts.no_bottom_toolbar = paramMap.getBoolean("noBottomToolbar");
                    if (paramMap.hasKey("noPhoneDialIn"))
                        opts.no_dial_in_via_phone = paramMap.getBoolean("noPhoneDialIn");
                    if (paramMap.hasKey("noPhoneDialOut"))
                        opts.no_dial_out_to_phone = paramMap.getBoolean("noPhoneDialOut");
                    if (paramMap.hasKey("noMeetingEndMessage"))
                        opts.no_meeting_end_message = paramMap.getBoolean("noMeetingEndMessage");
                    if (paramMap.hasKey("noMeetingErrorMessage"))
                        opts.no_meeting_error_message = paramMap.getBoolean("noMeetingErrorMessage");
                    if (paramMap.hasKey("noShare")) opts.no_share = paramMap.getBoolean("noShare");
                    if (paramMap.hasKey("noTitlebar"))
                        opts.no_titlebar = paramMap.getBoolean("noTitlebar");
                    if (paramMap.hasKey("customMeetingId"))
                        opts.custom_meeting_id = paramMap.getString("customMeetingId");
                    if (paramMap.hasKey("noDrivingMode"))
                        opts.no_driving_mode = paramMap.getBoolean("noDrivingMode");
                    if (paramMap.hasKey("noDisconnectAudio"))
                        opts.no_disconnect_audio = paramMap.getBoolean("noDisconnectAudio");
                    if (paramMap.hasKey("noRecord"))
                        opts.no_record = paramMap.getBoolean("noRecord");
                    if (paramMap.hasKey("noUnmuteConfirmDialog"))
                        opts.no_unmute_confirm_dialog = paramMap.getBoolean("noUnmuteConfirmDialog");
                    if (paramMap.hasKey("noWebinarRegisterDialog"))
                        opts.no_webinar_register_dialog = paramMap.getBoolean("noWebinarRegisterDialog");
                    if (paramMap.hasKey("noChatMsgToast"))
                        opts.no_chat_msg_toast = paramMap.getBoolean("noChatMsgToast");
                    if (paramMap.hasKey("noMeetingErrorMessage"))
                        opts.no_meeting_error_message = paramMap.getBoolean("noMeetingErrorMessage");

                    /** TODO: posible extra options:
                     opts.meeting_views_options = meetingOptions.meeting_views_options;
                     opts.invite_options = meetingOptions.invite_options;
                     opts.customer_key = meetingOptions.customer_key;
                     */

                    if (paramMap.hasKey("noButtonLeave") && paramMap.getBoolean("noButtonLeave"))
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_LEAVE;
                    if ((paramMap.hasKey("noButtonMore") && paramMap.getBoolean("noButtonMore")) || meetingMoreHidden)
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_MORE;
                    if (paramMap.hasKey("noButtonParticipants") && paramMap.getBoolean("noButtonParticipants"))
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_PARTICIPANTS;
                    if (paramMap.hasKey("noButtonShare") && paramMap.getBoolean("noButtonShare"))
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_SHARE;
                    if (paramMap.hasKey("noTextMeetingId") && paramMap.getBoolean("noTextMeetingId"))
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_MEETING_ID;
                    if (paramMap.hasKey("noTextPassword") && paramMap.getBoolean("noTextPassword"))
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_TEXT_PASSWORD;
                    if (meetingVideoHidden)
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_VIDEO;
                    if (meetingAudioHidden)
                        opts.meeting_views_options = opts.meeting_views_options + view.NO_BUTTON_AUDIO;

                    JoinMeetingParam4WithoutLogin params = new JoinMeetingParam4WithoutLogin();
                    params.displayName = paramMap.getString("userName");
                    params.meetingNo = paramMap.getString("meetingNumber");
                    if (paramMap.hasKey("password"))
                        params.password = paramMap.getString("password");
                    if (paramMap.hasKey("webinarToken"))
                        params.webinarToken = paramMap.getString("webinarToken");
                    if (paramMap.hasKey("zoomAccessToken"))
                        params.zoomAccessToken = paramMap.getString("zoomAccessToken");

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


                    List<?> userListRaw = zoomSDK.getInMeetingService().getInMeetingUserList();
                    if (userListRaw != null) {
                        for (Object userIdObj : userListRaw) {
                            if (userIdObj instanceof Long) {
                                rnUserList.pushString(String.valueOf(userIdObj));
                            } else {
                                rnUserList.pushString(userIdObj.toString());
                            }
                        }
                    }

                    promise.resolve(rnUserList);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION getInMeetingUserIdList", ex);
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

                        MobileRTCSDKError result = shareController.startShareViewContent(shareView);

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

        MobileRTCSDKError result = shareController.startShareScreen(intent);

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

    @ReactMethod
    public void sendChatMsg(final ReadableMap msgData, final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZoomSDK zoomSDK = ZoomSDK.getInstance();
                    if (!zoomSDK.isInitialized()) {
                        return;
                    }
                    InMeetingChatController inMeetingChatController = zoomSDK.getInMeetingService().getInMeetingChatController();
                    if (inMeetingChatController != null) {
                        ChatMessageBuilder chatMessageBuilder = new ChatMessageBuilder();

                        Handler handler = new Handler(Looper.getMainLooper());

                        String content;
                        int receiverId;
                        int chatTypeValue;
                        int hostId = 0;
                        String messageToHost = "";
                        String previousChatId = "";

                        content = msgData.getString("content");
                        receiverId = msgData.getInt("receiverId");
                        chatTypeValue = msgData.getInt("chatMessageType");
                        if (msgData.hasKey("hostId")) {
                            hostId = msgData.getType("hostId").name().equals("String") ? Integer.parseInt(msgData.getString("hostId")) : msgData.getInt("hostId");
                        }
                        if (msgData.hasKey("messageToHost")) {
                            messageToHost = msgData.getString("messageToHost");
                        }
                        if (msgData.hasKey("previousChatId")) {
                            previousChatId = msgData.getString("previousChatId");
                        }
                        ZoomSDKChatMessageType chatMessageType;
                        switch (chatTypeValue) {
                            case 0:
                                chatMessageType = ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_None;
                                break;
                            case 1:
                                chatMessageType = ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_All;
                                break;
                            case 2:
                                chatMessageType = ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_All_Panelist;
                                break;
                            case 3:
                                chatMessageType = ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_Individual_Panelist;
                                break;
                            case 4:
                                chatMessageType = ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_Individual;
                                break;
                            case 5:
                                chatMessageType = ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_WaitingRoomUsers;
                                break;
                            default:
                                promise.reject("ERR_INVALID_CHAT_TYPE", "Invalid chat message type: " + chatTypeValue);
                                return;
                        }
                        chatMessageBuilder.setContent(content);
                        chatMessageBuilder.setReceiver(receiverId);
                        chatMessageBuilder.setMessageType(chatMessageType);
                        InMeetingChatMessage inMeetingChatMessage = chatMessageBuilder.build();
                        inMeetingChatController.sendChatMsgTo(inMeetingChatMessage);
                        if (!messageToHost.isEmpty() && hostId != 0 && !previousChatId.isEmpty()) {
                            inMeetingChatController.deleteChatMessage(previousChatId);
                            Thread.sleep(2000);
                            chatMessageBuilder.setContent(messageToHost);
                            chatMessageBuilder.setReceiver(hostId);
                            chatMessageBuilder.setMessageType(ZoomSDKChatMessageType.ZoomSDKChatMessageType_To_Individual);
                            Thread.sleep(2000);
                            inMeetingChatController.sendChatMsgTo(inMeetingChatMessage);
                        }
                    }
                    promise.resolve(true);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

    @ReactMethod
    public void deleteChatMessage(final String msgId, final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ZoomSDK zoomSDK = ZoomSDK.getInstance();
                    if (!zoomSDK.isInitialized()) {
                        promise.resolve(false);
                        return;
                    }
                    InMeetingChatController inMeetingChatController = zoomSDK.getInMeetingService().getInMeetingChatController();
                    if (inMeetingChatController != null) {
                        inMeetingChatController.deleteChatMessage(msgId);
                    }
                    promise.resolve(true);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

    @ReactMethod
    public void isHostUser(final int userId, final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();
                    if (inMeetingService != null) {
                        promise.resolve(inMeetingService.isHostUser(userId));
                    }
                    promise.resolve(false);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

    @ReactMethod
    public void getMyselfUserID(final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();
                    if (inMeetingService != null) {
                        long userIdLong = inMeetingService.getMyUserID();
                        if (userIdLong >= Integer.MIN_VALUE && userIdLong <= Integer.MAX_VALUE) {
                            int userIdInt = (int) userIdLong;
                            promise.resolve(userIdInt);
                        } else {
                            promise.reject("ERR_USER_ID_OVERFLOW", "User ID is too large to convert to int.");
                        }
                    }
                    promise.resolve(null);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

    // Internal user list update trigger
    private void updateVideoView() {
        runUpdateVideoView(false);
    }

    private void runUpdateVideoView(boolean isRetry) {
        final UIManagerModule uiManager = reactContext.getNativeModule(UIManagerModule.class);

        if (uiManager == null) {
            if (retryCount < MAX_RETRY) {
                retryCount++;
                Log.w(TAG, "UIManagerModule is null, retry " + retryCount);
                new android.os.Handler(Looper.getMainLooper())
                        .postDelayed(() -> runUpdateVideoView(true), 300); // retry sau 300ms
            } else {
                Log.e(TAG, "UIManagerModule is still null after " + MAX_RETRY + " retries");
            }
            return;
        }

        retryCount = 0; // reset nếu thành công

        uiManager.addUIBlock(nativeViewHierarchyManager -> {
            synchronized (videoViews) {
                Log.i(TAG, "updateVideoView");
                Iterator<Integer> iterator = videoViews.iterator();
                while (iterator.hasNext()) {
                    final int tagId = iterator.next();
                    try {
                        final RNZoomUsVideoView view =
                                (RNZoomUsVideoView) nativeViewHierarchyManager.resolveView(tagId);
                        if (view != null) view.update();
                    } catch (Exception ex) {
                        Log.e(TAG, "Error updating video view", ex);
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
        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
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
            final ZoomUIService zoomUIServiceHelper = ZoomSDK.getInstance().getZoomUIService();
//      final MeetingViewsOptions
            if (meetingSettingsHelper != null) {
                meetingSettingsHelper.disableShowVideoPreviewWhenJoinMeeting(shouldDisablePreview);
                meetingSettingsHelper.setCustomizedMeetingUIEnabled(customizedMeetingUIEnabled);
                meetingSettingsHelper.disableClearWebKitCache(disableClearWebKitCache);
                meetingSettingsHelper.disableCopyMeetingUrl(true);
                meetingSettingsHelper.enable720p(true);
                meetingSettingsHelper.enableCloudWhiteboard(!disableCloudWhiteboard);
            }
            if (zoomUIServiceHelper != null) {
                zoomUIServiceHelper.hideMeetingInviteUrl(true);
            }
        }
    }

    @Override
    public void onZoomAuthIdentityExpired() {
    }

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

        if (meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
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
        if (meetingService != null) {
            Log.i(TAG, "registerListener, added listener for meetingService");
            meetingService.addListener(this);
        }
        InMeetingService inMeetingService = zoomSDK.getInMeetingService();
        if (inMeetingService != null) {
            Log.i(TAG, "registerListener, added listener for inMeetingService");
            inMeetingService.addListener(this);
            InMeetingShareController inMeetingShareController = inMeetingService.getInMeetingShareController();
            InMeetingBOController inMeetingBOController = inMeetingService.getInMeetingBOController();
            if (inMeetingShareController != null) {
                Log.i(TAG, "registerListener, added listener for getInMeetingShareController");
                inMeetingShareController.addListener(this);
            }
            if (inMeetingBOController != null) {
                Log.i(TAG, "registerListener, added listener for getInMeetingBOController");
                inMeetingBOController.addListener(this);
                mInMeetingBOController = inMeetingBOController;
            }
        }
    }

    private void unregisterListener() {
        Log.i(TAG, "unregisterListener");
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if (zoomSDK.isInitialized()) {
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

    public int getInMeetingUserCount() {
        final ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if (!zoomSDK.isInitialized()) {
            return 0;
        }

        final int inMeetingUserCount = zoomSDK.getInMeetingService().getInMeetingUserCount();
        sendEvent("MeetingEvent", "inMeetingUserCount", inMeetingUserCount);
        return inMeetingUserCount;
    }

    // InMeetingServiceListener required listeners
    @Override
    public void onMeetingLeaveComplete(long ret) {
        updateVideoView();
        sendEvent("MeetingEvent", getMeetingEndReasonName((int) ret));
    }

    @Override
    public void onMeetingUserJoin(List<Long> userIdList) {
        updateVideoView();
        sendEvent("MeetingEvent", "userJoin", userIdList);
        getInMeetingUserCount();
    }

    @Override
    public void onMeetingUserLeave(List<Long> userIdList) {
        updateVideoView();
        sendEvent("MeetingEvent", "userLeave", userIdList);
        getInMeetingUserCount();
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

    @Deprecated
    public void onMeetingCoHostChanged(long userId) {
    }

    @Override
    public void onMeetingCoHostChange(long userId, boolean isCoHost) {
        sendEvent("MeetingEvent", "coHostChanged", userId);
    }

    @Override
    public void onMyAudioSourceTypeChanged(int type) {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if (!zoomSDK.isInitialized()) {
            return;
        }
        final InMeetingUserInfo userInfo = ZoomSDK.getInstance().getInMeetingService().getMyUserInfo();

        sendEvent("MeetingEvent", "myAudioSourceTypeChanged", userInfo);
    }

    @Override
    public void onUserAudioStatusChanged(long userId, AudioStatus audioStatus) {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if (!zoomSDK.isInitialized()) {
            return;
        }
        InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();

        if (userId == inMeetingService.getMyUserID()) {
            final InMeetingUserInfo userInfo = inMeetingService.getMyUserInfo();

            sendEvent("MeetingEvent", "myAudioStatusChanged", userInfo);
        }
    }

    @Override
    public void onUserVideoStatusChanged(long userId, VideoStatus videoStatus) {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if (!zoomSDK.isInitialized()) {
            return;
        }
        InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();

        if (userId == inMeetingService.getMyUserID()) {
            final InMeetingUserInfo userInfo = inMeetingService.getMyUserInfo();

            sendEvent("MeetingEvent", "myVideoStatusChanged", userInfo);
        }
    }

    // InMeetingServiceListener required listeners but unused for now
    @Override
    public void onAllowParticipantsRequestCloudRecording(boolean bAllow) {
    }

    @Override
    public void onSinkJoin3rdPartyTelephonyAudio(String s) {

    }

    @Override
    public void onUserConfirmToStartArchive(IMeetingArchiveConfirmHandler iMeetingArchiveConfirmHandler) {

    }

    @Override
    public void onCameraControlRequestReceived(long l, CameraControlRequestType cameraControlRequestType, ICameraControlRequestHandler iCameraControlRequestHandler) {

    }

    @Override
    public void onCameraControlRequestResult(long l, boolean b) {

    }

    @Override
    public void onCameraControlRequestResult(long l, CameraControlRequestResult cameraControlRequestResult) {

    }

    @Override
    public void onFileSendStart(ZoomSDKFileSender zoomSDKFileSender) {

    }

    @Override
    public void onFileReceived(ZoomSDKFileReceiver zoomSDKFileReceiver) {

    }

    @Override
    public void onFileTransferProgress(ZoomSDKFileTransferInfo zoomSDKFileTransferInfo) {

    }

    @Override
    public void onMuteOnEntryStatusChange(boolean b) {

    }

    @Override
    public void onMeetingTopicChanged(String s) {

    }

    @Override
    public void onMeetingFullToWatchLiveStream(String s) {

    }

    @Override
    public void onBotAuthorizerRelationChanged(long l) {

    }

    @Override
    public void onVirtualNameTagStatusChanged(boolean b, long l) {

    }

    @Override
    public void onVirtualNameTagRosterInfoUpdated(long l) {

    }

    @Override
    public void onCreateCompanionRelation(long l, long l1) {

    }

    @Override
    public void onRemoveCompanionRelation(long l) {

    }

    @Override
    public void onUserConfirmRecoverMeeting(IRecoverMeetingHandle iRecoverMeetingHandle) {

    }

    @Override
    public void onUVCCameraStatusChange(String cameraId, UVCCameraStatus status) {
    }

    @Override
    public void onVideoAlphaChannelStatusChanged(boolean isAlphaModeOn) {
    }

    @Override
    public void onFocusModeStateChanged(boolean on) {
    }

    @Override
    public void onFocusModeShareTypeChanged(MobileRTCFocusModeShareType shareType) {
    }

    @Override
    public void onAICompanionActiveChangeNotice(boolean b) {
    }

    @Override
    public void onParticipantProfilePictureStatusChange(boolean b) {
    }

    @Override
    public void onCloudRecordingStorageFull(long l) {
    }

    @Override
    public void onInMeetingUserAvatarPathUpdated(long userId) {
    }

    @Override
    public void onSuspendParticipantsActivities() {
    }

    @Override
    public void onAllowParticipantsStartVideoNotification(boolean allow) {
    }

    @Override
    public void onAllowParticipantsRenameNotification(boolean allow) {
    }

    @Override
    public void onAllowParticipantsUnmuteSelfNotification(boolean allow) {
    }

    @Override
    public void onAllowParticipantsShareWhiteBoardNotification(boolean allow) {
    }

    @Override
    public void onMeetingLockStatus(boolean isLock) {
    }

    @Override
    public void onFollowHostVideoOrderChanged(boolean bFollow) {
    }

    @Override
    public void onMeetingParameterNotification(MeetingParameter meetingParameter) {
    }

    @Override
    public void onHostVideoOrderUpdated(List<Long> orderList) {
    }

    @Override
    public void onMeetingNeedPasswordOrDisplayName(boolean needPassword, boolean needDisplayName, InMeetingEventHandler handler) {
    }

    @Override
    public void onWebinarNeedRegister(String registerUrl) {
    }

    @Override
    public void onJoinMeetingNeedUserInfo(IMeetingInputUserInfoHandler iMeetingInputUserInfoHandler) {

    }

    @Override
    public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler handler) {
    }

    @Override
    public void onWebinarNeedInputScreenName(InMeetingEventHandler inMeetingEventHandler) {

    }

    @Override
    public void onMeetingNeedCloseOtherMeeting(InMeetingEventHandler handler) {
    }

    @Override
    public void onMeetingFail(int errorCode, int internalErrorCode) {
    }

    @Override
    public void onMeetingUserUpdated(long userId) {
    }

    @Override
    public void onActiveVideoUserChanged(long userId) {
    }

    @Override
    public void onActiveSpeakerVideoUserChanged(long userId) {
    }

    @Deprecated
    public void onSpotlightVideoChanged(boolean on) {
    }

    @Override
    public void onSpotlightVideoChanged(List<Long> userList) {
    }

    @Override
    public void onSinkPanelistChatPrivilegeChanged(InMeetingChatController.MobileRTCWebinarPanelistChatPrivilege privilege) {
    }

    @Deprecated
    public void onUserNetworkQualityChanged(long userId) {
    }

    ;

    @Override
    public void onSinkMeetingVideoQualityChanged(VideoQuality videoQuality, long userId) {
    }

    @Override
    public void onMicrophoneStatusError(InMeetingAudioController.MobileRTCMicrophoneError error) {
    }

    @Override
    public void onUserAudioTypeChanged(long userId) {
    }

    @Override
    public void onLowOrRaiseHandStatusChanged(long userId, boolean isRaisedHand) {
    }

    @Override
    public void onChatMessageReceived(InMeetingChatMessage inMeetingChatMessage) {
        sendEventChatInfo("MeetingEvent", "onChatMessageNotification", inMeetingChatMessage);
    }

    @Override
    public void onChatMsgDeleteNotification(String msgID, ChatMessageDeleteType deleteBy) {
        if (msgID != null) {
            WritableMap params = Arguments.createMap();
            params.putString("deleteBy", deleteBy.name());
            params.putString("msgID", msgID);

            WritableMap rootParams = Arguments.createMap();
            rootParams.putString("event", "onChatMsgDeleteNotification");
            rootParams.putMap("msgDelete", params);
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("MeetingEvent", rootParams);
        }
    }

    @Override
    public void onChatMessageEditNotification(InMeetingChatMessage inMeetingChatMessage) {

    }

    @Override
    public void onSilentModeChanged(boolean inSilentMode) {
    }

    @Override
    public void onMeetingActiveVideo(long userId) {
    }

    @Override
    public void onSinkAttendeeChatPrivilegeChanged(int i) {

    }

    @Override
    public void onSinkAllowAttendeeChatNotification(int privilege) {
    }

    @Deprecated
    public void onUserNameChanged(long userId, String name) {
    }

    @Override
    public void onUserNamesChanged(List<Long> userList) {
    }

    @Override
    public void onInvalidReclaimHostkey() {
    }

    @Override
    public void onRecordingStatus(RecordingStatus status) {
    }

    @Override
    public void onLocalRecordingStatus(long userId, RecordingStatus recordingStatus) {
    }

    @Override
    public void onClosedCaptionReceived(String message, long senderId) {
    }

    @Override
    public void onFreeMeetingReminder(boolean isHost, boolean canUpgrade, boolean isFirstGift) {
    }

    @Override
    public void onFreeMeetingUpgradeToProMeeting() {
    }

    @Override
    public void onFreeMeetingUpgradeToGiftFreeTrialStop() {
    }

    @Override
    public void onFreeMeetingUpgradeToGiftFreeTrialStart() {
    }

    @Override
    public void onFreeMeetingNeedToUpgrade(FreeMeetingNeedUpgradeType type, String gifUrl) {
    }

    @Override
    public void onLocalVideoOrderUpdated(List<Long> localOrderList) {
    }

    @Override
    public void onAllHandsLowered() {
    }

    @Override
    public void onPermissionRequested(String[] permissions) {
    }

    @Override
    public void onShareMeetingChatStatusChanged(boolean start) {
    }

    @Override
    public void onLocalRecordingPrivilegeRequested(IRequestLocalRecordingPrivilegeHandler handler) {
    }

    @Override
    public void onRequestLocalRecordingPrivilegeChanged(LocalRecordingRequestPrivilegeStatus status) {
    }

    // InMeetingShareListener event listeners
    // DEPRECATED: onShareActiveUser is just kept for now for backwards compatibility of events

    @Override
    public void onShareSettingTypeChanged(ShareSettingType type) {
    }

    @Override
    public void onShareContentChanged(ZoomSDKSharingSourceInfo zoomSDKSharingSourceInfo) {
        final ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if (!zoomSDK.isInitialized()) {
            return;
        }
        final InMeetingService inMeetingService = zoomSDK.getInMeetingService();
        if (inMeetingService == null) return;
        long userId = -1;

        if (zoomSDKSharingSourceInfo != null) {
            userId = zoomSDKSharingSourceInfo.getUserID();
        }
        if (inMeetingService.isMyself(userId)) {
            sendEvent("MeetingEvent", "screenShareStarted");
        } else if (userId == 0) {
            sendEvent("MeetingEvent", "screenShareStopped");
        }
    }

    @Override
    public void onSharingStatus(ZoomSDKSharingSourceInfo zoomSDKSharingSourceInfo) {
        updateVideoView();
        SharingStatus status = SharingStatus.Sharing_Status_None;
        long userId = -1;

        if (zoomSDKSharingSourceInfo != null) {
            if (zoomSDKSharingSourceInfo.getStatus() != null) {
                status = zoomSDKSharingSourceInfo.getStatus();
            }
            userId = zoomSDKSharingSourceInfo.getUserID();
        }

        sendEvent("MeetingEvent", getSharingStatusEventName(status), userId);

        if (status.equals(SharingStatus.Sharing_Self_Send_Begin)) {
            final InMeetingService inMeetingService = ZoomSDK.getInstance().getInMeetingService();
            final InMeetingShareController shareController = inMeetingService.getInMeetingShareController();

            if (shareController.isSharingOut()) {
                if (shareController.isSharingScreen()) {
                    shareController.startShareViewContent(shareView);
                }
            }
        }
    }

    @Override
    public void onShareUserReceivingStatus(long userId) {
    }

    @Override
    public void onGrantCoOwnerPrivilegeChanged(boolean canGrantOther) {

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
                    if (!zoomSDK.isInitialized()) {
                        return;
                    }

                    final MeetingService meetingService = zoomSDK.getMeetingService();
                    List<MeetingStatus> staleMeetingStatuses = new ArrayList<>(Arrays.asList(MeetingStatus.MEETING_STATUS_IDLE, MeetingStatus.MEETING_STATUS_DISCONNECTING));
                    if (!staleMeetingStatuses.contains(meetingService.getMeetingStatus())) {
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
    public void invalidate() {
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

    private void sendEventChatInfo(String name, String event, InMeetingChatMessage msg) {

        if (msg != null) {
            WritableMap params = Arguments.createMap();
            params.putString("chatId", msg.getMsgId());
            params.putString("senderId", String.valueOf(msg.getSenderUserId()));
            params.putString("senderName", msg.getSenderDisplayName());
            params.putString("receiverId", String.valueOf(msg.getReceiverUserId()));
            params.putString("receiverName", msg.getReceiverDisplayName());
            params.putString("content", msg.getContent());
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = sdf.format(new Date(msg.getTime()));
            params.putString("date", formattedDate);
            params.putInt("chatMessageType", msg.getChatMessageType().hashCode());
            params.putBoolean("isChatToAll", msg.isChatToAll());
            params.putBoolean("isChatToWaitingroom", msg.isChatToWaitingroom());
            params.putBoolean("isChatToAllPanelist", msg.isChatToAllPanelist());
            params.putBoolean("isComment", msg.isComment());
            params.putBoolean("isThread", msg.isThread());
            params.putString("threadId", msg.getThreadId());

            WritableMap rootParams = Arguments.createMap();
            rootParams.putString("event", event);
            rootParams.putMap("chatInfo", params);

            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(name, rootParams);
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

    private void sendEvent(String name, String event, String status) {
        WritableMap params = Arguments.createMap();
        params.putString("event", event);
        params.putString("status", status);

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
            case Sharing_Self_Send_Begin:
                return "screenShareStartedBySelf";
            case Sharing_Self_Send_End:
                return "screenShareStoppedBySelf";
            case Sharing_Other_Share_Begin:
                return "screenShareStartedByUser";
            case Sharing_Other_Share_End:
                return "screenShareStoppedByUser";
            case Sharing_View_Other_Sharing:
                return "screenShareOtherSharing";
            case Sharing_Pause:
                return "screenSharePause";
            case Sharing_Resume:
                return "screenShareResume";
            default:
                return "screenShareStoppedByUser";
        }
    }

    private String getAuthErrorName(final int errorCode) {
        switch (errorCode) {
            case ZoomError.ZOOM_ERROR_AUTHRET_CLIENT_INCOMPATIBLE:
                return "clientIncompatible";
            case ZoomError.ZOOM_ERROR_SUCCESS:
                return "success";
            case ZoomError.ZOOM_ERROR_DEVICE_NOT_SUPPORTED:
                return "deviceNotSupported"; // Android only
            case ZoomError.ZOOM_ERROR_ILLEGAL_APP_KEY_OR_SECRET:
                return "illegalAppKeyOrSecret"; // Android only
            case ZoomError.ZOOM_ERROR_INVALID_ARGUMENTS:
                return "invalidArguments"; // Android only
            case ZoomError.ZOOM_ERROR_NETWORK_UNAVAILABLE:
                return "networkUnavailable"; // Android only
            default:
                return "unknown";
        }
    }

    private String getMeetErrorName(final int errorCode) {
        switch (errorCode) {
            case MeetingError.MEETING_ERROR_INVALID_ARGUMENTS:
                return "invalidArguments";
            case MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE:
                return "meetingClientIncompatible";
            case MeetingError.MEETING_ERROR_LOCKED:
                return "meetingLocked";
            case MeetingError.MEETING_ERROR_MEETING_NOT_EXIST:
                return "meetingNotExist";
            case MeetingError.MEETING_ERROR_MEETING_OVER:
                return "meetingOver";
            case MeetingError.MEETING_ERROR_RESTRICTED:
                return "meetingRestricted";
            case MeetingError.MEETING_ERROR_RESTRICTED_JBH:
                return "meetingRestrictedJBH";
            case MeetingError.MEETING_ERROR_USER_FULL:
                return "meetingUserFull";
            case MeetingError.MEETING_ERROR_MMR_ERROR:
                return "mmrError";
            case MeetingError.MEETING_ERROR_NO_MMR:
                return "noMMR";
            case MeetingError.MEETING_ERROR_HOST_DENY_EMAIL_REGISTER_WEBINAR:
                return "registerWebinarDeniedEmail";
            case MeetingError.MEETING_ERROR_WEBINAR_ENFORCE_LOGIN:
                return "registerWebinarEnforceLogin";
            case MeetingError.MEETING_ERROR_REGISTER_WEBINAR_FULL:
                return "registerWebinarFull";
            case MeetingError.MEETING_ERROR_DISALLOW_HOST_REGISTER_WEBINAR:
                return "registerWebinarHostRegister";
            case MeetingError.MEETING_ERROR_DISALLOW_PANELIST_REGISTER_WEBINAR:
                return "registerWebinarPanelistRegister";
            case MeetingError.MEETING_ERROR_REMOVED_BY_HOST:
                return "removedByHost";
            case MeetingError.MEETING_ERROR_SESSION_ERROR:
                return "sessionError";
            case MeetingError.MEETING_ERROR_SUCCESS:
                return "success";
            case MeetingError.MEETING_ERROR_EXIT_WHEN_WAITING_HOST_START:
                return "exitWhenWaitingHostStart"; // Android only
            case MeetingError.MEETING_ERROR_INCORRECT_MEETING_NUMBER:
                return "incorrectMeetingNumber"; // Android only
            case MeetingError.MEETING_ERROR_INVALID_STATUS:
                return "invalidStatus"; // Android only
            case MeetingError.MEETING_ERROR_NETWORK_UNAVAILABLE:
                return "networkUnavailable"; // Android only
            case MeetingError.MEETING_ERROR_TIMEOUT:
                return "timeout"; // Android only
            case MeetingError.MEETING_ERROR_WEB_SERVICE_FAILED:
                return "webServiceFailed"; // Android only
            default:
                return "unknown";
        }
    }

    private String getMeetingEndReasonName(final int reason) {
        switch (reason) {
            case MeetingEndReason.END_BY_SELF:
                return "endedBySelf";
            case MeetingEndReason.KICK_BY_HOST:
                return "endedRemovedByHost";
            case MeetingEndReason.END_BY_HOST:
                return "endedByHost";
            case MeetingEndReason.END_FOR_JBH_TIMEOUT:
                return "endedJBHTimeout";
            case MeetingEndReason.END_FOR_FREEMEET_TIMEOUT:
                return "endedFreeMeetingTimeout";
            case MeetingEndReason.END_FOR_NO_ATEENDEE:
                return "endedNoAttendee"; // Android only
            case MeetingEndReason.END_BY_HOST_START_ANOTHERMEETING:
                return "endedByHostForAnotherMeeting";
            case MeetingEndReason.END_UNDEFINED:
                return "endedConnectBroken";
            default:
                return "endedUnknownReason";
        }
    }

    @Override
    public void onHasCreatorRightsNotification(IBOCreator iboCreator) {
        Log.i(TAG, "onHasCreatorRightsNotification");
    }

    @Override
    public void onHasAdminRightsNotification(IBOAdmin iboAdmin) {
        Log.i(TAG, "onHasAdminRightsNotification");
    }

    @Override
    public void onHasAssistantRightsNotification(IBOAssistant iboAssistant) {
        Log.i(TAG, "onHasAssistantRightsNotification");
    }

    @Override
    public void onHasAttendeeRightsNotification(IBOAttendee iboAttendee) {
        Log.i(TAG, "onHasAttendeeRightsNotification");
        String boName = iboAttendee.getBoName();
        if (boName != null && !boName.isEmpty()) {
            sendEvent("MeetingEvent", "onHasAttendeeRightsNotification", boName);
        } else {
            sendEvent("MeetingEvent", "onHasAttendeeRightsNotification", "");
        }
        iboAttendee.setEvent(new IBOAttendeeEvent() {

            public void onShareAction(SharingStatus status, IShareAction shareAction) {

            }

            @Override
            public void onHelpRequestHandleResultReceived(ATTENDEE_REQUEST_FOR_HELP_RESULT eResult) {
                Log.i(TAG, "onHelpRequestHandleResultReceived:" + eResult);
            }

            @Override
            public void onHostJoinedThisBOMeeting() {
                Log.i(TAG, "onHostJoinedThisBOMeeting:");
            }

            @Override
            public void onHostLeaveThisBOMeeting() {
                Log.i(TAG, "onHostLeaveThisBOMeeting:");
            }
        });
    }

    @Override
    public void onHasDataHelperRightsNotification(IBOData iboData) {
        Log.i(TAG, "onHasDataHelperRightsNotification");
    }

    @Override
    public void onLostCreatorRightsNotification() {
        Log.i(TAG, "onLostCreatorRightsNotification");
    }

    @Override
    public void onLostAdminRightsNotification() {
        Log.i(TAG, "onLostAdminRightsNotification");
    }

    @Override
    public void onLostAssistantRightsNotification() {
        Log.i(TAG, "onLostAssistantRightsNotification");
    }

    @Override
    public void onLostAttendeeRightsNotification() {
        Log.i(TAG, "onLostAttendeeRightsNotification");
    }

    @Override
    public void onLostDataHelperRightsNotification() {
        Log.i(TAG, "onLostDataHelperRightsNotification");
    }

    @Override
    public void onNewBroadcastMessageReceived(String message, long senderId, String senderName) {
        Log.i(TAG, "onNewBroadcastMessageReceived:" + message + " senderId:" + senderId + " senderName:" + senderName);

    }

    @Override
    public void onBOStopCountDown(int seconds) {
        Log.i(TAG, "onBOStopCountDown seconds: " + seconds);
    }

    @Override
    public void onHostInviteReturnToMainSession(String name, ReturnToMainSessionHandler returnToMainSessionHandler) {
        Log.i(TAG, "onHostInviteReturnToMainSession name: " + name);
    }

    @Override
    public void onBOStatusChanged(BOStatus boStatus) {
        Log.i(TAG, "onBOStatusChanged status: " + boStatus);
        sendEvent("MeetingEvent", "onBOStatusChanged", boStatus == BOStatus.STARTED ? "MobileRTCBOStatus_Started" : boStatus.name());

    }

    @Override
    public void onBOSwitchRequestReceived(String strNewBOName, String strNewBOID) {
        Log.i(TAG, "onBOSwitchRequestReceived: boName: " + strNewBOName + ", boID: " + strNewBOID);
    }

    @Override
    public void onBroadcastBOVoiceStatus(boolean start) {
        Log.i(TAG, "onBroadcastBOVoiceStatus " + start);
    }

    @Override
    public void onBOOptionChanged(BOOption boOption) {
        Log.i(TAG, "onBOOptionChanged " + boOption);
    }

    @Override
    public void onShareFromMainSession(long l, SharingStatus sharingStatus, IShareAction iShareAction) {

    }

    @ReactMethod
    public void joinBO(final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ZoomSDK zoomSDK = ZoomSDK.getInstance();
                    if (!zoomSDK.isInitialized()) {
                        promise.resolve(null);
                        return;
                    }
                    InMeetingService inMeetingService = zoomSDK.getInMeetingService();
                    InMeetingBOController boController = inMeetingService.getInMeetingBOController();
                    IBOAttendee boAttendee = boController.getBOAttendeeHelper();
                    if (boAttendee == null) {
                        promise.reject("BO_UNASSIGNED", "");
                        return;
                    }
                    boolean ret = boAttendee.joinBo();
                    promise.resolve(ret);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

    @ReactMethod
    public void leaveBO(final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

                    if (!zoomSDK.isInitialized()) {
                        promise.resolve(null);
                        return;
                    }
                    InMeetingService inMeetingService = zoomSDK.getInMeetingService();
                    InMeetingBOController boController = inMeetingService.getInMeetingBOController();
                    IBOAttendee boAttendee = boController.getBOAttendeeHelper();
                    if (boAttendee == null) {
                        return;
                    }
                    boolean ret = boAttendee.leaveBo();
                    promise.resolve(ret);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

    @ReactMethod
    public void isHostInThisBO(final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

                    if (!zoomSDK.isInitialized()) {
                        promise.resolve(null);
                        return;
                    }
                    InMeetingService inMeetingService = zoomSDK.getInMeetingService();
                    InMeetingBOController boController = inMeetingService.getInMeetingBOController();
                    IBOAttendee boAttendee = boController.getBOAttendeeHelper();
                    if (boAttendee == null) {
                        return;
                    }
                    boolean ret = boAttendee.isHostInThisBO();
                    promise.resolve(ret);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

    @ReactMethod
    public void requestForHelp(final Promise promise) {
        UiThreadUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ZoomSDK zoomSDK = ZoomSDK.getInstance();

                    if (!zoomSDK.isInitialized()) {
                        promise.resolve(null);
                        return;
                    }
                    InMeetingService inMeetingService = zoomSDK.getInMeetingService();
                    InMeetingBOController boController = inMeetingService.getInMeetingBOController();
                    IBOAttendee boAttendee = boController.getBOAttendeeHelper();
                    if (boAttendee == null) {
                        return;
                    }
                    boolean ret = boAttendee.requestForHelp();
                    promise.resolve(ret);
                } catch (Exception ex) {
                    promise.reject("ERR_UNEXPECTED_EXCEPTION", ex);
                }
            }
        });
    }

}
