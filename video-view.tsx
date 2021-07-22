import React, { useRef, useEffect } from 'react'
import { View, findNodeHandle } from 'react-native'
import Color from 'color'
import { RNZoomUs, RNZoomUsVideoView } from './native'

export interface LayoutUnit {
  kind: "active" | "preview" | "share" | "attendee"
  x: number
  y: number
  width: number
  height: number
  border?: boolean
  showUsername?: boolean
  showAudioOff?: boolean
  userIndex?: number
  background?: string
}

export interface Props {
  layout: LayoutUnit[]
}

const ZoomVideoView: React.FC<Props> = (props) => {
  if (RNZoomUsVideoView) {
    const { layout } = props

    const nativeLayout = layout.map(unit => ({
      ...unit,
      x: Math.ceil(unit.x * 100),
      y: Math.ceil(unit.y * 100),
      width: Math.ceil(unit.width * 100),
      height: Math.ceil(unit.height * 100),
      background: Color(unit.background || '#000000').rgbNumber(),
    }))

    const nativeEl = useRef(null)

    useEffect(() => {
      if (nativeEl.current) {
        RNZoomUs.addVideoView(findNodeHandle(nativeEl.current))
      }

      return () => {
        if (nativeEl.current) {
          RNZoomUs.removeVideoView(findNodeHandle(nativeEl.current))
        }
      }
    }, [nativeEl.current])

    return (
      <RNZoomUsVideoView
        ref={nativeEl}
        {...props}
        layout={nativeLayout}
      />
    )
  } else {
    return (<View {...props} />)
  }
}

export default ZoomVideoView
