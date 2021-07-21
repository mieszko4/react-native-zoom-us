import {
  NativeModules,
  HostComponent,
  requireNativeComponent,
  Platform,
} from 'react-native'

export interface NativeLayoutUnit {
  kind: "active" | "preview" | "share" | "attendee"
  x: number
  y: number
  width: number
  height: number
  border?: boolean
  show_username?: boolean
  show_audio_off?: boolean
  user_index?: number
  background?: number
}

export interface NativeVideoProps {
  layout: NativeLayoutUnit[]
}

export const RNZoomUsVideoView = (
  Platform.OS === 'android' ? requireNativeComponent('RNZoomUsVideoView') : null
) as HostComponent<NativeVideoProps> | null

export const RNZoomUs = NativeModules.RNZoomUs
