import * as React from 'react'
import { View, findNodeHandle } from 'react-native'
import Color from 'color'
import { RNZoomUs, RNZoomUsVideoView } from './native'

export interface LayoutUnit {
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
}

export interface Props {
  layout: LayoutUnit[]
}

const ZoomUsVideoView: React.FC<Props> = (props) => {
  const { layout = [], ...otherProps } = props

  const nativeLayout = layout.map(unit => {
    return Object.assign({}, unit, {
      x: Math.ceil(unit.x * 100),
      y: Math.ceil(unit.y * 100),
      width: Math.ceil(unit.width * 100),
      height: Math.ceil(unit.height * 100),
      background: Color(unit.background || '#000000').rgbNumber(),
    })
  })

  const nativeEl = React.useRef(null)

  React.useEffect(() => {
    (async function register() {
      if (nativeEl.current) {
        await RNZoomUs.addVideoView(findNodeHandle(nativeEl.current))
      }
    })()

    return () => {
      (async function unregister() {
        if (nativeEl.current) {
          await RNZoomUs.removeVideoView(findNodeHandle(nativeEl.current))
        }
      })()
    }
  }, [nativeEl.current])

  if (RNZoomUsVideoView) {
    return (
      <RNZoomUsVideoView
        ref={nativeEl}
        layout={nativeLayout}
        {...otherProps}
      />
    )
  } else {
    return (<View {...otherProps} />)
  }
}

export default ZoomUsVideoView
