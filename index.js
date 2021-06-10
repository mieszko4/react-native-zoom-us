"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
  return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ZoomEmitter = void 0;
const react_native_1 = require("react-native");
const invariant_1 = __importDefault(require("invariant"));
const { RNZoomUs } = react_native_1.NativeModules;
if (!RNZoomUs)
  console.error('RNZoomUs native module is not linked.');
const DEFAULT_USER_TYPE = 2;
async function initialize(params, settings = {
  // more details inside: https://github.com/mieszko4/react-native-zoom-us/issues/28
  disableShowVideoPreviewWhenJoinMeeting: true,
}) {
  invariant_1.default(typeof params === 'object', 'ZoomUs.initialize expects object param. Consider to check migration docs. ' +
    'Check Link: https://github.com/mieszko4/react-native-zoom-us/blob/master/docs/UPGRADING.md');
  if ('jwtToken' in params) {
    invariant_1.default(params.jwtToken, 'ZoomUs.initialize requires jwtToken');
  }
  else {
    invariant_1.default(params.clientKey, 'ZoomUs.initialize requires clientKey');
    invariant_1.default(params.clientSecret, 'ZoomUs.initialize requires clientSecret');
  }
  if (!params.domain)
    params.domain = 'zoom.us';
  return RNZoomUs.initialize(params, settings);
}
async function joinMeeting(params) {
  let { meetingNumber, noAudio = false, noVideo = false, autoConnectAudio = false } = params;
  invariant_1.default(meetingNumber, 'ZoomUs.joinMeeting requires meetingNumber');
  if (typeof meetingNumber !== 'string')
    meetingNumber = meetingNumber.toString();
  // without noAudio, noVideo fields SDK can stack on joining meeting room for release build
  return RNZoomUs.joinMeeting({
    ...params,
    meetingNumber,
    noAudio: !!noAudio,
    noVideo: !!noVideo,
    autoConnectAudio,
  });
}
async function joinMeetingWithPassword(...params) {
  console.warn("ZoomUs.joinMeetingWithPassword is deprecated. Use joinMeeting({ password: 'xxx', ... })");
  return RNZoomUs.joinMeetingWithPassword(...params);
}
async function startMeeting(params) {
  let { userType = DEFAULT_USER_TYPE, meetingNumber } = params;
  invariant_1.default(meetingNumber, 'ZoomUs.startMeeting requires meetingNumber');
  if (typeof meetingNumber !== 'string')
    meetingNumber = meetingNumber.toString();
  return RNZoomUs.startMeeting({ userType, ...params, meetingNumber });
}
async function leaveMeeting() {
  return RNZoomUs.leaveMeeting();
}
async function connectAudio() {
  return RNZoomUs.connectAudio();
}
exports.ZoomEmitter = RNZoomUs;
exports.default = {
  initialize,
  joinMeeting,
  joinMeetingWithPassword,
  startMeeting,
  leaveMeeting,
  connectAudio,
};
