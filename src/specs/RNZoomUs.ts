import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';
import { RNZoomUsInitializeParams, RNZoomUsJoinMeetingParams, RNZoomUsStartMeetingParams } from '../..';

type InitializeSettings = {
  language?: Language;
  enableCustomizedMeetingUI?: boolean;
  disableShowVideoPreviewWhenJoinMeeting?: boolean;
  disableMinimizeMeeting?: boolean;
  disableClearWebKitCache?: boolean;
};

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


export interface Spec extends TurboModule {
  addVideoView: (tag: number) => Promise<string>;
  removeVideoView: (tag: number) => Promise<string>;
  startMeeting: (options: RNZoomUsStartMeetingParams) => Promise<string>;
  joinMeeting: (options: RNZoomUsJoinMeetingParams) => Promise<string>;
  leaveMeeting: () => Promise<string>;
  connectAudio: () => Promise<string>;
  isMeetingConnected: () => Promise<string>;
  isMeetingHost: () => Promise<string>;
  getInMeetingUserIdList: () => Promise<string>;
  muteMyVideo: (muted: boolean) => Promise<string>;
  rotateMyVideo: (rotation: number) => Promise<string>;
  muteMyAudio: (muted: boolean) => Promise<string>;
  muteAttendee: (userId: string, muted: boolean) => Promise<string>;
  muteAllAttendee: (allowUnmuteSelf: boolean) => Promise<string>;
  startShareScreen: () => Promise<string>;
  stopShareScreen: () => Promise<string>;
  switchCamera: () => Promise<string>;
  raiseMyHand: () => Promise<string>;
  lowerMyHand: () => Promise<string>;
  addListener: (eventName: string) => void;
  removeListeners: (count: number) => void;
  isInitialized: () => Promise<string>;
  initialize: (options: RNZoomUsInitializeParams, settings: InitializeSettings) => Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('RNZoomUs');
