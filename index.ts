import { Platform } from "react-native";
import invariant from "invariant";
import { RNZoomUs } from "./native";
import events from "./src/events";

const DEFAULT_USER_TYPE = 2;

type Language =
  | "de"
  | "ja"
  | "en"
  | "zh-Hant"
  | "es"
  | "zh-Hans"
  | "it"
  | "ko"
  | "vi"
  | "ru"
  | "pt-PT"
  | "fr";

const applyLanguageMapping = (language: Language): string => {
  const androidLanguageMapping = {
    "zh-Hans": "zh-CN",
    "zh-Hant": "zh-TW",
  };
  if (Platform.OS === "android") {
    return androidLanguageMapping[language] || language;
  }

  return language;
};
interface RNZoomUsInitializeCommonParams {
  domain?: string;
  iosAppGroupId?: string;
  iosScreenShareExtensionId?: string;
}
export interface RNZoomUsInitializeParams
  extends RNZoomUsInitializeCommonParams {
  jwtToken: string;
}

type InitializeSettings = {
  language?: Language;
  enableCustomizedMeetingUI?: boolean;
  disableShowVideoPreviewWhenJoinMeeting?: boolean;
  disableMinimizeMeeting?: boolean;
  disableClearWebKitCache?: boolean;
  meetingShareHidden?: boolean;
  meetingVideoHidden?: boolean;
  meetingAudioHidden?: boolean;
  closeCaptionHidden?: boolean;
  disableCloudWhiteboard?: boolean;
  meetingMoreHidden?: boolean;
};

export interface ZoomChatInfo {
  chatId?: string
  chatMessageType?: number
  content: string
  date?: string
  isChatToAll?: boolean
  isChatToAllPanelist?: boolean
  isChatToWaitingroom?: boolean
  isComment?: boolean
  isMyself?: boolean
  isPrivate?: boolean
  isThread?: boolean
  receiverId: number
  receiverName?: string
  senderId?: string
  senderName?: string
  threadID?: string
  hostId?: number
  messageToHost?: string
  previousChatId?: string
}

async function initialize(
  {
    domain = "zoom.us",
    ...params
  }: RNZoomUsInitializeParams,
  {
    language = "en",
    enableCustomizedMeetingUI = false,

    // ios only
    // more details inside: https://github.com/mieszko4/react-native-zoom-us/issues/28
    disableShowVideoPreviewWhenJoinMeeting = true,

    // ios only
    disableMinimizeMeeting = false,

    disableClearWebKitCache = false,
    ...settings
  }: InitializeSettings = {}
): Promise<string> {
  invariant(
    typeof params === "object",
    "ZoomUs.initialize expects object param. Consider to check migration docs. " +
    "Check Link: https://github.com/mieszko4/react-native-zoom-us/blob/master/docs/UPGRADING.md"
  );

  invariant(params.jwtToken, "ZoomUs.initialize requires jwtToken");

  const mappedSettings = {
    language: applyLanguageMapping(language),
    enableCustomizedMeetingUI,

    disableShowVideoPreviewWhenJoinMeeting,

    disableMinimizeMeeting,
    disableClearWebKitCache,
    ...settings
  };

  const mappedParams = {
    domain,
    ...params,
  };

  return RNZoomUs.initialize(mappedParams, mappedSettings);
}

function cleanup(): Promise<string> {
  return RNZoomUs.cleanup();
}

function isInitialized(): Promise<boolean> {
  return RNZoomUs.isInitialized();
}

export interface RNZoomUsJoinMeetingParams {
  userName: string;
  meetingNumber: string | number;
  password?: string;
  autoConnectAudio?: boolean;
  noAudio?: boolean;
  noVideo?: boolean;

  noButtonLeave?: boolean;
  noButtonMore?: boolean;
  noButtonParticipants?: boolean;
  noButtonShare?: boolean;
  noTextMeetingId?: boolean;
  noTextPassword?: boolean;
  webinarToken?: string;
  zoomAccessToken?: string;

  // android only fields:
  noInvite?: boolean;
  noBottomToolbar?: boolean;
  noPhoneDialIn?: boolean;
  noPhoneDialOut?: boolean;
  noMeetingEndMessage?: boolean;
  noMeetingErrorMessage?: boolean;
  noShare?: boolean;
  noTitlebar?: boolean;
  noDrivingMode?: boolean;
  noDisconnectAudio?: boolean;
  noRecord?: boolean;
  noUnmuteConfirmDialog?: boolean;
  noWebinarRegisterDialog?: boolean;
  noChatMsgToast?: boolean;
}
async function joinMeeting(params: RNZoomUsJoinMeetingParams) {
  let {
    meetingNumber,
    noAudio = false,
    noVideo = false,
    autoConnectAudio = false,
  } = params;
  invariant(meetingNumber, "ZoomUs.joinMeeting requires meetingNumber");
  if (typeof meetingNumber !== "string")
    meetingNumber = meetingNumber.toString();

  // without noAudio, noVideo fields SDK can stack on joining meeting room for release build
  return RNZoomUs.joinMeeting({
    ...params,
    meetingNumber,
    noAudio: !!noAudio, // required
    noVideo: !!noVideo, // required
    autoConnectAudio, // required
  });
}

async function joinMeetingWithPassword(
  userName: RNZoomUsJoinMeetingParams["userName"],
  meetingNumber: NonNullable<RNZoomUsJoinMeetingParams["meetingNumber"]>,
  password: NonNullable<RNZoomUsJoinMeetingParams["password"]>
) {
  console.warn(
    "ZoomUs.joinMeetingWithPassword is deprecated. Use joinMeeting({ password: 'xxx', ... })"
  );
  return joinMeeting({
    userName,
    meetingNumber,
    password,
  });
}

export interface RNZoomUsStartMeetingParams {
  userName: string;
  meetingNumber: string | number;
  userType?: number; // looks like can be different for IOS and Android
  zoomAccessToken: string;

  // android only fields:
  noInvite?: boolean;
  noShare?: boolean;
  noMeetingErrorMessage?: boolean;

  noButtonLeave?: boolean;
  noButtonMore?: boolean;
  noButtonParticipants?: boolean;
  noButtonShare?: boolean;
  noTextMeetingId?: boolean;
  noTextPassword?: boolean;
}
async function startMeeting(params: RNZoomUsStartMeetingParams) {
  let { userType = DEFAULT_USER_TYPE, meetingNumber } = params;

  invariant(meetingNumber, "ZoomUs.startMeeting requires meetingNumber");
  if (typeof meetingNumber !== "string")
    meetingNumber = meetingNumber.toString();

  return RNZoomUs.startMeeting({ userType, ...params, meetingNumber });
}

async function leaveMeeting() {
  return RNZoomUs.leaveMeeting();
}

async function connectAudio() {
  return RNZoomUs.connectAudio();
}

async function isMeetingConnected() {
  return RNZoomUs.isMeetingConnected();
}

async function isMeetingHost() {
  return RNZoomUs.isMeetingHost();
}

async function getInMeetingUserIdList() {
  return RNZoomUs.getInMeetingUserIdList();
}

async function rotateMyVideo(rotation: number) {
  if (Platform.OS === "android") {
    return RNZoomUs.rotateMyVideo(rotation);
  } else {
    throw new Error("Only support android");
  }
}

async function muteMyVideo(muted: boolean) {
  return RNZoomUs.muteMyVideo(muted);
}

async function muteMyAudio(muted: boolean) {
  return RNZoomUs.muteMyAudio(muted);
}

async function muteAttendee(userId: string, muted: boolean) {
  return RNZoomUs.muteAttendee(userId, muted);
}

async function muteAllAttendee(allowUnmuteSelf: boolean) {
  return RNZoomUs.muteAllAttendee(allowUnmuteSelf);
}

async function startShareScreen() {
  return RNZoomUs.startShareScreen();
}

async function stopShareScreen() {
  return RNZoomUs.stopShareScreen();
}

async function switchCamera() {
  return RNZoomUs.switchCamera();
}

async function raiseMyHand() {
  return RNZoomUs.raiseMyHand();
}

async function lowerMyHand() {
  return RNZoomUs.lowerMyHand();
}

async function joinBO() {
  return RNZoomUs.joinBO();
}

async function leaveBO() {
  return RNZoomUs.leaveBO();
}

async function requestForHelp() {
  return RNZoomUs.requestForHelp();
}

async function isHostInThisBO() {
  return RNZoomUs.isHostInThisBO();
}

async function getAllChatMessageID() {
  return RNZoomUs.getAllChatMessageID();
}

async function sendChatMsg(msgData: ZoomChatInfo) {
  return RNZoomUs.sendChatMsg(msgData);
}

async function deleteChatMessage(msgId: string)  {
  return RNZoomUs.deleteChatMessage(msgId);
}

async function isHostUser({ userId }: { userId: number }) {
  return RNZoomUs.isHostUser(userId);
}

async function getMyselfUserID(): Promise<number> {
  return RNZoomUs.getMyselfUserID();
}



export { default as ZoomUsVideoView } from "./video-view";

export * from "./src/events";
export default {
  initialize,
  cleanup,
  joinMeeting,
  joinMeetingWithPassword,
  startMeeting,
  leaveMeeting,
  connectAudio,
  isInitialized,
  isMeetingHost,
  isMeetingConnected,
  getInMeetingUserIdList,
  rotateMyVideo,
  muteMyVideo,
  muteMyAudio,
  muteAttendee,
  muteAllAttendee,
  startShareScreen,
  stopShareScreen,
  switchCamera,
  raiseMyHand,
  lowerMyHand,
  joinBO,
  leaveBO,
  requestForHelp,
  isHostInThisBO,
  getAllChatMessageID,
  sendChatMsg,
  isHostUser,
  deleteChatMessage,
  getMyselfUserID,
  ...events,
};
