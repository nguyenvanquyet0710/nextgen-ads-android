/*
 * Copyright 2026 NextGen Ads
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nextgen.ads.nativead

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.R
import com.nextgen.ads.callbacks.AdEventListener

/**
 * Fullscreen Activity that displays a Native Ad.
 * Used for the "Inter → Native Fullscreen → action" combo pattern.
 *
 * Launch via [NativeFullScreenActivity.launch] — do NOT create intents manually.
 */
class NativeFullScreenActivity : AppCompatActivity() {

  private lateinit var flNative: FrameLayout
  private lateinit var btnClose: ImageView

  override fun onCreate(savedInstanceState: Bundle?) {
    // Immersive fullscreen mode
    window.navigationBarColor = Color.TRANSPARENT
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = (
      View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_FULLSCREEN
        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
      )

    super.onCreate(savedInstanceState)
    setContentView(R.layout.nextgen_activity_native_fullscreen)

    // Prevent App Open Ad from showing on top
    NextGenAds.isFullScreenAdShowing = true

    flNative = findViewById(R.id.fl_native)
    btnClose = findViewById(R.id.btn_close)

    // Apply status bar height padding
    val statusBarView = findViewById<View>(R.id.view_status_bar)
    val statusHeight = getStatusBarHeight()
    statusBarView.layoutParams = statusBarView.layoutParams.apply {
      height = statusHeight
    }

    // Close button - show immediately
    btnClose.visibility = View.VISIBLE
    btnClose.setOnClickListener {
      finish()
    }

    // Get parameters (use default library layout if none provided)
    val layoutResId = intent.getIntExtra(EXTRA_LAYOUT_RES_ID, R.layout.nextgen_native_fullscreen)
    val adUnitId = intent.getStringExtra(EXTRA_AD_UNIT_ID)

    // Show the native ad
    val preloadedAd = pendingNativeAd
    pendingNativeAd = null

    if (preloadedAd != null) {
      // Use preloaded ad
      showNativeAd(preloadedAd, layoutResId)
    } else if (adUnitId != null) {
      // Load new ad
      loadAndShowNativeAd(adUnitId, layoutResId)
    } else {
      NextGenAds.logError("NativeFullScreenActivity: no ad or adUnitId provided")
      finish()
    }
  }

  private fun showNativeAd(nativeAd: NativeAd, @LayoutRes layoutResId: Int) {
    try {
      val inflater = layoutInflater
      val adView = inflater.inflate(layoutResId, flNative, false) as NativeAdView

      NativeAdHelper.autoBindViews(adView)
      NativeAdHelper.populateNativeAdView(nativeAd, adView)

      flNative.removeAllViews()
      flNative.addView(adView)

      NextGenAds.log("NativeFullScreenActivity: native ad displayed")
    } catch (e: Exception) {
      NextGenAds.logError("NativeFullScreenActivity: failed to show ad: ${e.message}", e)
      finish()
    }
  }

  private fun loadAndShowNativeAd(adUnitId: String, @LayoutRes layoutResId: Int) {
    NextGenAds.log("NativeFullScreenActivity: loading native ad...")
    NativeAdHelper.load(adUnitId) { nativeAd, error ->
      if (nativeAd != null) {
        NextGenAds.runOnMainThread {
          if (!isFinishing && !isDestroyed) {
            showNativeAd(nativeAd, layoutResId)
          } else {
            nativeAd.destroy()
          }
        }
      } else {
        NextGenAds.logError("NativeFullScreenActivity: failed to load: ${error?.message}")
        NextGenAds.runOnMainThread { finish() }
      }
    }
  }

  @Suppress("DEPRECATION")
  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      window.decorView.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
          or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          or View.SYSTEM_UI_FLAG_FULLSCREEN
          or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
  }

  override fun onDestroy() {
    NextGenAds.isFullScreenAdShowing = false
    pendingOnDismiss?.invoke()
    pendingOnDismiss = null
    super.onDestroy()
  }

  @Suppress("DEPRECATION")
  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    // Block back press — user must tap close button
  }

  private fun getStatusBarHeight(): Int {
    val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
  }

  companion object {
    private const val EXTRA_LAYOUT_RES_ID = "nextgen_layout_native"
    private const val EXTRA_AD_UNIT_ID = "nextgen_ad_unit_id"

    /** Preloaded NativeAd to display (set before launching). */
    internal var pendingNativeAd: NativeAd? = null

    /** Callback invoked when the activity is destroyed (closed). */
    internal var pendingOnDismiss: (() -> Unit)? = null

    /**
     * Launches the fullscreen native ad activity.
     *
     * @param activity The current Activity.
     * @param adUnitId The native ad unit ID (used if no preloaded ad).
     * @param layoutResId The native ad layout resource.
     * @param nativeAd Optional preloaded NativeAd (if null, will load a new one).
     * @param onDismiss Called when the user closes the fullscreen ad.
     */
    fun launch(
      activity: Activity,
      adUnitId: String,
      @LayoutRes layoutResId: Int,
      nativeAd: NativeAd? = null,
      onDismiss: () -> Unit,
    ) {
      pendingNativeAd = nativeAd
      pendingOnDismiss = onDismiss
      val intent = Intent(activity, NativeFullScreenActivity::class.java).apply {
        putExtra(EXTRA_LAYOUT_RES_ID, layoutResId)
        putExtra(EXTRA_AD_UNIT_ID, adUnitId)
      }
      activity.startActivity(intent)
    }
  }
}
