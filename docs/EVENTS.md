## Using NativeEventEmitter

In addition to a promise-based API, NativeEventEmitter can be used to subscribe to authentication
and meeting events as follows:

```javascript
import { ZoomEmitter } from 'react-native-zoom-us';

const zoomEmitter = new NativeEventEmitter(ZoomEmitter);

// Handle Auth events
zoomEmitter.addListener('Auth', (authEvent) => {
  // Note that the listener will receive an object with an `event` property
  console.log('Event: "' + authEvent.event + '"');
  // Will print one of the following:
  //   Event: "clientIncompatible"
  //   Event: "success"
  //   Event: "accountNotEnableSDK"
  //   Event: "accountNotSupport"
  //   Event: "keyOrSecretEmpty"
  //   Event: "keyOrSecretWrong"
  //   Event: "networkIssue"
  //   Event: "none"
  //   Event: "overTime"
  //   Event: "serviceBusy"
  //   Event: "unknown"
  //   Event: "deviceNotSupported"
  //   Event: "illegalAppKeyOrSecret"
  //   Event: "invalidArguments"
  //   Event: "networkUnavailable"
});

// Handle Meeting events
zoomEmitter.addListener('Meeting', (meetingEvent) => {
  // Note that the listener will receive an object with an `event` property
  console.log('Event: "' + meetingEvent.event + '"');
  // Will print one of the following ("ended*" events specifically identify the reason for a meeting
  // ending):
  //   Event: "invalidArguments"
  //   Event: "meetingClientIncompatible"
  //   Event: "meetingLocked"
  //   Event: "meetingNotExist"
  //   Event: "meetingOver"
  //   Event: "meetingRestricted"
  //   Event: "meetingRestrictedJBH"
  //   Event: "meetingUserFull"
  //   Event: "mmrError"
  //   Event: "networkError"
  //   Event: "noMMR"
  //   Event: "registerWebinarDeniedEmail"
  //   Event: "registerWebinarEnforceLogin"
  //   Event: "registerWebinarFull"
  //   Event: "registerWebinarHostRegister"
  //   Event: "registerWebinarPanelistRegister"
  //   Event: "removedByHost"
  //   Event: "sessionError"
  //   Event: "success"
  //   Event: "audioAutoStartError"
  //   Event: "cannotEmitWebRequest"
  //   Event: "cannotStartTokenExpire"
  //   Event: "inAnotherMeeting"
  //   Event: "invalidUserType"
  //   Event: "joinWebinarWithSameEmail"
  //   Event: "meetingNotStart"
  //   Event: "passwordError"
  //   Event: "reconnectError"
  //   Event: "vanityNotExist"
  //   Event: "vbMaximumNum"
  //   Event: "vbNoSupport"
  //   Event: "vbRemoveNone"
  //   Event: "vbSaveImage"
  //   Event: "vbSetError"
  //   Event: "videoError"
  //   Event: "writeConfigFile"
  //   Event: "zcCertificateChanged"
  //   Event: "unknown"
  //   Event: "exitWhenWaitingHostStart"
  //   Event: "incorrectMeetingNumber"
  //   Event: "invalidStatus"
  //   Event: "networkUnavailable"
  //   Event: "timeout"
  //   Event: "webServiceFailed"
  //   Event: "endedByHost"
  //   Event: "endedByHostForAnotherMeeting"
  //   Event: "endedBySelf"
  //   Event: "endedConnectBroken"
  //   Event: "endedFreeMeetingTimeout"
  //   Event: "endedJBHTimeout"
  //   Event: "endedRemovedByHost"
  //   Event: "endedUnknownReason"
  //   Event: "endedNoAttendee"
});

```
