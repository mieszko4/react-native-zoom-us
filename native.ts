import {
  HostComponent,
  requireNativeComponent,
  Platform,
  StyleProp,
  ViewStyle,
} from 'react-native'
import RNZoomUs from './src/specs/RNZoomUs'

export const VideoAspectModeEnum = {
  VIDEO_ASPECT_ORIGINAL: 0,
  VIDEO_ASPECT_FULL_FILLED: 1,
  VIDEO_ASPECT_LETTER_BOX: 2,
  VIDEO_ASPECT_PAN_AND_SCAN: 3,
  VIDEO_ASPECT_TRUSTEE: 4,
} as const

type Values<T> = T[keyof T];
export type VideoAspectMode = Values<typeof VideoAspectModeEnum>;

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
  aspectMode?: VideoAspectMode
}

export interface NativeVideoProps {
  style?: StyleProp<ViewStyle>
  layout: NativeLayoutUnit[]
}

// TODO: implement for iOS -> https://github.com/mieszko4/react-native-zoom-us/issues/113
export const RNZoomUsVideoView = (
  Platform.OS === 'android' ? requireNativeComponent('RNZoomUsVideoView') : null
) as HostComponent<NativeVideoProps> | null

if (!RNZoomUs) console.error('react-native-zoom-us: RNZoomUs is undefined. Make sure the library is linked on the native side.')
export default RNZoomUs
