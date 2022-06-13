import {
  NativeModules,
  HostComponent,
  requireNativeComponent,
  Platform,
  StyleProp,
  ViewStyle,
} from 'react-native'

export enum VideoAspectModeEnum {
  VIDEO_ASPECT_ORIGINAL = 0,
  VIDEO_ASPECT_FULL_FILLED = 1,
  VIDEO_ASPECT_LETTER_BOX = 2,
  VIDEO_ASPECT_PAN_AND_SCAN = 3,
  VIDEO_ASPECT_TRUSTEE = 4,
}

export interface NativeLayoutUnit {
  kind: "active" | "preview" | "share" | "attendee" | "active-share"
  x: number
  y: number
  width: number
  height: number
  border?: boolean
  showUsername?: boolean
  showAudioOff?: boolean
  userIndex?: number
  background?: string
  aspectMode?: VideoAspectModeEnum // buggy typing
}

export interface NativeVideoProps {
  style?: StyleProp<ViewStyle>
  layout: NativeLayoutUnit[]
}

// TODO: implement for iOS -> https://github.com/mieszko4/react-native-zoom-us/issues/113
export const RNZoomUsVideoView = (
  Platform.OS === 'android' ? requireNativeComponent('RNZoomUsVideoView') : null
) as HostComponent<NativeVideoProps> | null

export const RNZoomUs = NativeModules.RNZoomUs
if (!RNZoomUs) console.error('[react-native-zoom-us] RNZoomUs native module is not linked.')

export default RNZoomUs
