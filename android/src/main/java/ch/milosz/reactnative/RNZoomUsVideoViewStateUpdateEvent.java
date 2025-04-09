package ch.milosz.reactnative;

import androidx.annotation.NonNull;
import com.facebook.react.uimanager.events.Event;

public class RNZoomUsVideoViewStateUpdateEvent extends Event<RNZoomUsVideoViewStateUpdateEvent> {
    public static final String EVENT_NAME = "onZoomVideoViewStateUpdate";

    public RNZoomUsVideoViewStateUpdateEvent(int viewId) {
        super(viewId);
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void dispatch(com.facebook.react.uimanager.events.RCTEventEmitter rctEventEmitter) {
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), null);
    }
}
