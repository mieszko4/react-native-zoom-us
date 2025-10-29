import {
  NativeModules,
  HostComponent,
  requireNativeComponent,
  Platform,
  StyleProp,
  ViewStyle,
} from "react-native";

export const VideoAspectModeEnum = {
  VIDEO_ASPECT_ORIGINAL: 0,
  VIDEO_ASPECT_FULL_FILLED: 1,
  VIDEO_ASPECT_LETTER_BOX: 2,
  VIDEO_ASPECT_PAN_AND_SCAN: 3,
  VIDEO_ASPECT_TRUSTEE: 4,
} as const;

type Values<T> = T[keyof T];
export type VideoAspectMode = Values<typeof VideoAspectModeEnum>;

export interface NativeLayoutUnit {
  kind: "active" | "preview" | "share" | "attendee" | "active-share";
  x: number;
  y: number;
  width: number;
  height: number;
  border?: boolean;
  showUsername?: boolean;
  showAudioOff?: boolean;
  userIndex?: number;
  background?: string;
  aspectMode?: VideoAspectMode;
}

export interface NativeVideoProps {
  style?: StyleProp<ViewStyle>;
  layout: NativeLayoutUnit[];
  muteMyCamera?: boolean;
  muteMyAudio?: boolean;
  onSinkMeetingUserJoin?: (event: any) => void;
  onSinkMeetingUserLeft?: (event: any) => void;
  onMeetingStateChange?: (event: any) => void;
  onInMeetingUserCount?: (event: any) => void;
  onBOStatusChanged?: (event: any) => void;
  onHasAttendeeRightsNotification?: (event: any) => void;
  onMeetingAudioRequestUnmuteByHost?: (event: any) => void;
  onMeetingVideoRequestUnmuteByHost?: (event: any) => void;
  onSinkMeetingAudioStatusChange?: (event: any) => void;
  onMeetingPreviewStopped?: (event: any) => void;
  onSinkMeetingVideoStatusChange?: (event: any) => void;
  onChatMessageNotification?: (event: any) => void;
  onChatMsgDeleteNotification?: (event: any) => void;
}

// TODO: implement for iOS -> https://github.com/mieszko4/react-native-zoom-us/issues/113
export const RNZoomUsVideoView = (
  Platform.OS === "android"
    ? requireNativeComponent("RNZoomUsVideoView")
    : requireNativeComponent("RNZoomUsVideoView")
) as HostComponent<NativeVideoProps> | null;

export const RNZoomUs = NativeModules.RNZoomUs;
if (!RNZoomUs)
  console.error("[react-native-zoom-us] RNZoomUs native module is not linked.");
