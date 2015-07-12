package com.codeblast.signage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.codeblast.signage.util.SystemUiHider;

import me.grantland.widget.AutofitHelper;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {

  /**
   * Whether or not the system UI should be auto-hidden after
   * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
   */
  private static final boolean AUTO_HIDE = true;

  /**
   * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
   * user interaction before hiding the system UI.
   */
  private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

  /**
   * If set, will toggle the system UI visibility upon interaction. Otherwise,
   * will show the system UI visibility upon interaction.
   */
  private static final boolean TOGGLE_ON_CLICK = true;

  /**
   * The flags to pass to {@link SystemUiHider#getInstance}.
   */
  private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
  private static final String TAG = "FullscreenActivity";
  Handler mHideHandler = new Handler();
  /**
   * The instance of the {@link SystemUiHider} for this activity.
   */
  private SystemUiHider mSystemUiHider;
  Runnable mHideRunnable = new Runnable() {
    @Override
    public void run() {
      mSystemUiHider.hide();
      logFontMetrics();
    }
  };
  /**
   * Touch listener to use for in-layout UI controls to delay hiding the
   * system UI. This is to prevent the jarring behavior of controls going away
   * while interacting with activity UI.
   */
  View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
      if (AUTO_HIDE) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
      }
      return false;
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_fullscreen);

    final View controlsView = findViewById(R.id.fullscreen_content_controls);
    final View contentView = findViewById(R.id.message);

    // Set up an instance of SystemUiHider to control the system UI for
    // this activity.
    mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
    mSystemUiHider.setup();
    mSystemUiHider
        .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
          // Cached values.
          int mControlsHeight;

          int mShortAnimTime;

          @Override
          @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
          public void onVisibilityChange(boolean visible) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
              // If the ViewPropertyAnimator API is available
              // (Honeycomb MR2 and later), use it to animate the
              // in-layout UI controls at the bottom of the
              // screen.
              if (mControlsHeight == 0) {
                mControlsHeight = controlsView.getHeight();
              }
              if (mShortAnimTime == 0) {
                mShortAnimTime = getResources().getInteger(
                    android.R.integer.config_shortAnimTime);
              }
              controlsView.animate()
                  .translationY(visible ? 0 : mControlsHeight)
                  .setDuration(mShortAnimTime);
            } else {
              // If the ViewPropertyAnimator APIs aren't
              // available, simply show or hide the in-layout UI
              // controls.
              controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            if (visible && AUTO_HIDE) {
              // Schedule a hide().
              delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
          }
        });

    // Set up the user interaction to manually show or hide the system UI.
    contentView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (TOGGLE_ON_CLICK) {
          mSystemUiHider.toggle();
        } else {
          mSystemUiHider.show();
        }
      }
    });

    AutofitHelper.create((TextView) contentView)
        .setMaxLines(4) // TODO: we can guess a an appropriate number based on the number of words in the text etc.
        .setMaxTextSize(280) // TODO: this should probably roughly screenHeight / maxLines? Store in dimens.xml ...
        .setMinTextSize(10);

    // Upon interacting with UI controls, delay any scheduled hide()
    // operations to prevent the jarring behavior of controls going away
    // while interacting with the UI.
    findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
  }

  private void logFontMetrics() {
    final DisplayMetrics metrics = getResources().getDisplayMetrics();
    Log.d(TAG, " density=" + metrics.density);
    Log.d(TAG, " heightPixels=" + metrics.heightPixels);
    Log.d(TAG, " widthPixels=" + metrics.widthPixels);
    Log.d(TAG, " heightDp=" + metrics.heightPixels / metrics.density);
    Log.d(TAG, " widthDp=" + metrics.widthPixels / metrics.density);
    final Configuration config = getResources().getConfiguration();
    Log.d(TAG, " configHeightDp=" + config.screenHeightDp);
    Log.d(TAG, " configWidthDp=" + config.screenWidthDp);
    Log.d(TAG, " fontScale=" + config.fontScale);
    final TextView contentView = (TextView) findViewById(R.id.message);
    Log.d(TAG, " textSizePixels=" + contentView.getTextSize());
    Log.d(TAG, " textSizeSp=" + contentView.getTextScaleX() * metrics.density * config.fontScale);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // Trigger the initial hide() shortly after the activity has been
    // created, to briefly hint to the user that UI controls
    // are available.
    delayedHide(100);
  }

  /**
   * Schedules a call to hide() in [delay] milliseconds, canceling any
   * previously scheduled calls.
   */
  private void delayedHide(int delayMillis) {
    mHideHandler.removeCallbacks(mHideRunnable);
    mHideHandler.postDelayed(mHideRunnable, delayMillis);
  }
}
