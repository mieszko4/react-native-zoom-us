import { NativeEventEmitter, NativeModule } from 'react-native'
import { RNZoomUs } from '../native'

export const ZoomEmitter = RNZoomUs as NativeModule
const EventEmitter = new NativeEventEmitter(ZoomEmitter)

// Android statuses took from https://zoom.github.io/zoom-sdk-android/us/zoom/sdk/MeetingStatus.html#MEETING_STATUS_INMEETING
type AndroidStatusEvent =
    | 'MEETING_STATUS_IDLE'
    | 'MEETING_STATUS_WAITINGFORHOST'
    | 'MEETING_STATUS_CONNECTING'
    | 'MEETING_STATUS_INMEETING'
    | 'MEETING_STATUS_DISCONNECTING'
    | 'MEETING_STATUS_RECONNECTING'
    | 'MEETING_STATUS_FAILED'
    | 'MEETING_STATUS_IN_WAITING_ROOM'
    | 'MEETING_STATUS_WEBINAR_PROMOTE'
    | 'MEETING_STATUS_WEBINAR_DEPROMOTE'
    | 'MEETING_STATUS_UNKNOWN'

// does these extra events exists in android? Maybe different listeners.
type IOSStatusExtraEvent =
    | 'MEETING_STATUS_WAITING_EXTERNAL_SESSION_KEY'
    | 'MEETING_STATUS_ENDED'
    | 'MEETING_STATUS_LOCKED'
    | 'MEETING_STATUS_UNLOCKED'
    | 'MEETING_STATUS_JOIN_BO'
    | 'MEETING_STATUS_LEAVE_BO'

type IOSStatusEvent = AndroidStatusEvent | IOSStatusExtraEvent

export type ZoomUsMeetingStatusEvent = AndroidStatusEvent | IOSStatusEvent

function onMeetingStatusChange(fn: (data: { event: ZoomUsMeetingStatusEvent }) => any) {
  return EventEmitter.addListener('MeetingStatus', (data: { event: ZoomUsMeetingStatusEvent }) => {
    // here we can add extra params, if needed
    fn(data)
  })
}

function onMeetingJoined(fn: () => any) {
  return onMeetingStatusChange(({ event }) => {
    if(event === 'MEETING_STATUS_INMEETING') {
      fn()
    }
  })
}

export default {
  onMeetingStatusChange,
  onMeetingJoined,
}
