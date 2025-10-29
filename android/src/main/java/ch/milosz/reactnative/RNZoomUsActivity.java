package ch.milosz.reactnative;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.NewMeetingActivity;

public class RNZoomUsActivity extends NewMeetingActivity {
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    onClickLeave();
  }
}
