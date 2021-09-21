import {
  NativeModules,
  HostComponent,
  requireNativeComponent,
  Platform,
  StyleProp,
  ViewStyle,
} from 'react-native'

export interface NativeLayoutUnit {
  style?: StyleProp<ViewStyle>
  kind: "active" | "preview" | "share" | "attendee" | "active-share"
  x: number
  y: number
  width: number
  height: number
  border?: boolean
  showUsername?: boolean
  showAudioOff?: boolean
  userIndex?: number
  background?: number
}

export interface NativeVideoProps {
  layout: NativeLayoutUnit[]
}

// TODO: implement for iOS -> https://github.com/mieszko4/react-native-zoom-us/issues/113
export const RNZoomUsVideoView = (
  Platform.OS === 'android' ? requireNativeComponent('RNZoomUsVideoView') : null
) as HostComponent<NativeVideoProps> | null

export const RNZoomUs = NativeModules.RNZoomUs
