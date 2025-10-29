import * as React from "react";
import { View, findNodeHandle } from "react-native";
import Color from "color";
import { RNZoomUs, RNZoomUsVideoView, NativeVideoProps } from "./native";

export interface Props {
  style?: NativeVideoProps["style"];
  layout: NativeVideoProps["layout"];
  muteMyCamera?: boolean
  muteMyAudio?: boolean
  fullScreen?: boolean
  onSinkMeetingUserJoin?: (event: any) => void;
  onSinkMeetingUserLeft?: (event: any) => void;
  onMeetingStateChange?: (event: any) => void;
  onInMeetingUserCount?: (event: any) => void;
  onBOStatusChanged?: (event: any) => void;
  onHasAttendeeRightsNotification?: (event: any) => void;
  onMeetingAudioRequestUnmuteByHost?: (event: any) => void;
  onMeetingVideoRequestUnmuteByHost?: (event: any) => void;
  onSinkMeetingAudioStatusChange?: (event: any) => void;
  onMeetingPreviewStopped?: (event: any) => void;
  onSinkMeetingVideoStatusChange?: (event: any) => void;
  onChatMessageNotification?: (event: any) => void;
  onChatMsgDeleteNotification?: (event: any) => void;
}

const ZoomUsVideoView: React.FC<Props> = (props) => {
  const {
    layout = [],
    onSinkMeetingUserLeft,
    onSinkMeetingUserJoin,
    onMeetingStateChange,
    onInMeetingUserCount,
    onBOStatusChanged,
    onHasAttendeeRightsNotification,
    onMeetingAudioRequestUnmuteByHost,
    onMeetingVideoRequestUnmuteByHost,
    onSinkMeetingAudioStatusChange,
    onMeetingPreviewStopped,
    onSinkMeetingVideoStatusChange,
    onChatMessageNotification,
    onChatMsgDeleteNotification,
    ...otherProps
  } = props;

  const nativeLayout = layout.map((unit) => {
    return Object.assign({}, unit, {
      x: Math.ceil(unit.x * 100),
      y: Math.ceil(unit.y * 100),
      width: Math.ceil(unit.width * 100),
      height: Math.ceil(unit.height * 100),
      background: Color(unit.background || "#000000").rgbNumber(),
    });
  });

  const nativeEl = React.useRef(null);

  React.useEffect(() => {
    (async function register() {
      if (nativeEl.current) {
        await RNZoomUs.addVideoView(findNodeHandle(nativeEl.current));
      }
    })();

    return () => {
      (async function unregister() {
        if (nativeEl.current) {
          await RNZoomUs.removeVideoView(findNodeHandle(nativeEl.current));
        }
      })();
    };
  }, [nativeEl.current]);

  if (RNZoomUsVideoView) {
    return (
      <RNZoomUsVideoView
        ref={nativeEl}
        layout={nativeLayout}
        onSinkMeetingUserJoin={(event) =>
          onSinkMeetingUserJoin?.(event?.["nativeEvent"])
        }
        onSinkMeetingUserLeft={(event) =>
          onSinkMeetingUserLeft?.(event?.["nativeEvent"])
        }
        onMeetingStateChange={(event) =>
          onMeetingStateChange?.(event?.["nativeEvent"])
        }
        onInMeetingUserCount={(event) =>
          onInMeetingUserCount?.(event?.["nativeEvent"])
        }
        onBOStatusChanged={(event) =>
          onBOStatusChanged?.(event?.["nativeEvent"])
        }
        onHasAttendeeRightsNotification={(event) =>
          onHasAttendeeRightsNotification?.(event?.["nativeEvent"])
        }
        onMeetingAudioRequestUnmuteByHost={(event) =>
          onMeetingAudioRequestUnmuteByHost?.(event?.["nativeEvent"])
        }
        onMeetingVideoRequestUnmuteByHost={(event) =>
          onMeetingVideoRequestUnmuteByHost?.(event?.["nativeEvent"])
        }
        onSinkMeetingAudioStatusChange={(event) =>
          onSinkMeetingAudioStatusChange?.(event?.["nativeEvent"])
        }
        onMeetingPreviewStopped={(event) =>
          onMeetingPreviewStopped?.(event?.["nativeEvent"])
        }
        onSinkMeetingVideoStatusChange={(event) =>
          onSinkMeetingVideoStatusChange?.(event?.["nativeEvent"])
        }
        onChatMessageNotification={(event) =>
          onChatMessageNotification?.(event?.["nativeEvent"])
        }
        onChatMsgDeleteNotification={(event) =>
          onChatMsgDeleteNotification?.(event?.["nativeEvent"])
        }
        {...otherProps}
      />
    );
  } else {
    return <View {...otherProps} />;
  }
};

export default ZoomUsVideoView;
