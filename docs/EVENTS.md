## Using NativeEventEmitter

In addition to a promise-based API, NativeEventEmitter can be used to subscribe to authentication
and meeting events as follows:

Remember that you should add the listener after the sdk has been initialized successfully.

```ts
import { ZoomEmitter } from 'react-native-zoom-us';

const zoomEmitter = new NativeEventEmitter(ZoomEmitter);

// Handle Auth events
zoomEmitter.addListener('AuthEvent', (authEvent) => {
  console.log(authEvent);
  // {event: 'clientIncompatible'}
  // {event: 'success'}
  // {event: 'accountNotEnableSDK'}
  // {event: 'accountNotSupport'}
  // {event: 'keyOrSecretEmpty'}
  // {event: 'keyOrSecretWrong'}
  // {event: 'networkIssue'}
  // {event: 'none'}
  // {event: 'overTime'}
  // {event: 'serviceBusy'}
  // {event: 'unknown'}
  // {event: 'deviceNotSupported'}
  // {event: 'illegalAppKeyOrSecret'}
  // {event: 'invalidArguments'}
  // {event: 'networkUnavailable'}
});

// Handle Meeting events
zoomEmitter.addListener('MeetingEvent', (meetingEvent) => {
  console.log(meetingEvent);
  // {event: 'invalidArguments'}
  // {event: 'meetingClientIncompatible'}
  // {event: 'meetingLocked'}
  // {event: 'meetingNotExist'}
  // {event: 'meetingOver'}
  // {event: 'meetingRestricted'}
  // {event: 'meetingRestrictedJBH'}
  // {event: 'meetingUserFull'}
  // {event: 'mmrError'}
  // {event: 'networkError'}
  // {event: 'noMMR'}
  // {event: 'registerWebinarDeniedEmail'}
  // {event: 'registerWebinarEnforceLogin'}
  // {event: 'registerWebinarFull'}
  // {event: 'registerWebinarHostRegister'}
  // {event: 'registerWebinarPanelistRegister'}
  // {event: 'removedByHost'}
  // {event: 'sessionError'}
  // {event: 'success'}
  // {event: 'audioAutoStartError'}
  // {event: 'cannotEmitWebRequest'}
  // {event: 'cannotStartTokenExpire'}
  // {event: 'inAnotherMeeting'}
  // {event: 'invalidUserType'}
  // {event: 'joinWebinarWithSameEmail'}
  // {event: 'meetingNotStart'}
  // {event: 'passwordError'}
  // {event: 'reconnectError'}
  // {event: 'vanityNotExist'}
  // {event: 'vbMaximumNum'}
  // {event: 'vbNoSupport'}
  // {event: 'vbRemoveNone'}
  // {event: 'vbSaveImage'}
  // {event: 'vbSetError'}
  // {event: 'videoError'}
  // {event: 'writeConfigFile'}
  // {event: 'zcCertificateChanged'}
  // {event: 'unknown'}
  // {event: 'exitWhenWaitingHostStart'}
  // {event: 'incorrectMeetingNumber'}
  // {event: 'invalidStatus'}
  // {event: 'networkUnavailable'}
  // {event: 'timeout'}
  // {event: 'webServiceFailed'}
  
  // Will print one of the following ("ended*" events identify the reason for a meeting ending)
  // {event: 'endedByHost'}
  // {event: 'endedByHostForAnotherMeeting'}
  // {event: 'endedBySelf'}
  // {event: 'endedConnectBroken'}
  // {event: 'endedFreeMeetingTimeout'}
  // {event: 'endedJBHTimeout'}
  // {event: 'endedRemovedByHost'}
  // {event: 'endedUnknownReason'}
  // {event: 'endedNoAttendee'}
  
  // Meeting user events
  // {event: 'userJoin', userList: [userId]}
  // {event: 'userLeave', userList: [userId]}
  // {event: 'hostChanged', userId: number}
  // {event: 'coHostChanged', userId: number}
  // {event: 'endedNoAttendee'}

  // Meeting audio events
  // {event: 'myAudioTypeChanged', userRole: string, audioType: number, isMutedAudio: boolean, isTalking: boolean, isMutedVideo: boolean}
  // {event: 'myAudioStatusChanged', {userRole: string, audioType: number, isMutedAudio: boolean, isTalking: boolean, isMutedVideo: boolean}}
  
  // Meeting video events
  // {event: 'myVideoStatusChanged', userRole: string, audioType: number, isMutedAudio: boolean, isTalking: boolean, isMutedVideo: boolean}
  
  // Meeting screen share events
  // {event: 'screenShareStartedBySelf', userId: number}
  // {event: 'screenShareStoppedBySelf', userId: number}
  // {event: 'screenShareStartedByUser', userId: number}
  // {event: 'screenShareStoppedByUser', userId: number}
  // {event: 'screenShareOtherSharing', userId: number}
  // {event: 'screenSharePause', userId: number}
  // {event: 'screenShareResume', userId: number}
  // {event: 'screenShareStarted'} // DEPRECATED
  // {event: 'screenShareStopped'} // DEPRECATED
  
  
  // ANDROID ONLY EVENTS
  // {event: 'askUnMuteAudio'}
  // {event: 'askUnMuteVideo'}
  // {event: 'screenShareSuccess'} called on custom ui when share screen is successful
  // {event: 'screenShareError', error: string} called on custom ui when there was an error sharing screen
});

```
