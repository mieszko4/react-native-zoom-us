import {
  NativeModules,
  HostComponent,
  requireNativeComponent,
  Platform,
  StyleProp,
  ViewStyle,
} from 'react-native'

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
  aspectMode?: 0 | 1 | 2 | 3 | 4
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
