package ch.milosz.reactnative;

import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.MeetingActivity;
import android.view.WindowManager;
import android.os.Bundle;

public class RNZoomUsActivity extends MeetingActivity {

  /*onCreate Added by Sridhar*/
  @Override
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
  }

  @Override
  public void onBackPressed() {
    onClickLeave();
  }
}