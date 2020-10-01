import { NativeModules } from 'react-native'

const { RNZoomUs } = NativeModules

if (!RNZoomUs) console.error('RNZoomUs native module is not linked.')

async function initialize(
  params: { clientKey: string; clientSecret: string; domain?: string },
  settings?: {
    // ios only
    disableShowVideoPreviewWhenJoinMeeting?: boolean
  },
) {
  if (typeof params !== 'object') {
    throw new Error(
      'ZoomUs.initialize expect object param. Consider to check migration docs. ' +
        'Check Link: https://github.com/mieszko4/react-native-zoom-us/blob/master/docs/UPGRADING.md',
    )
  }

  if (!params.domain) params.domain = 'zoom.us'

  return RNZoomUs.initialize(params, {
    // more details inside: https://github.com/mieszko4/react-native-zoom-us/issues/28
    disableShowVideoPreviewWhenJoinMeeting: true,
    ...settings,
  })
}

async function joinMeeting(params: {
  userName: string
  meetingNumber: string | number
  password?: string
  participantID?: string
  noAudio?: boolean
  noVideo?: boolean

  // IOS only fields:
  zak?: string
  webinarToken?: string
}) {
  if (!params.meetingNumber) throw new Error('ZoomUs.joinMeeting requires meetingNumber')

  return RNZoomUs.joinMeeting(params)
}

async function joinMeetingWithPassword(...params) {
  console.warn("ZoomUs.joinMeetingWithPassword is deprecated. Use joinMeeting({ password: 'xxx', ... })")
  return RNZoomUs.joinMeetingWithPassword(...params)
}

export default {
  ...RNZoomUs,
  initialize,
  joinMeeting,
  joinMeetingWithPassword,
}
