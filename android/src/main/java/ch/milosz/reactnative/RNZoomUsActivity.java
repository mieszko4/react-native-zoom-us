package ch.milosz.reactnative;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.MeetingActivity;

public class RNZoomUsActivity extends MeetingActivity {
  @Override
  public void onBackPressed() {
    onClickLeave();
  }
}