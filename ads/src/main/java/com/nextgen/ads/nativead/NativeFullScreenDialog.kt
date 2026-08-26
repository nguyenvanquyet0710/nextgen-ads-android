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
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.LayoutRes
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.nextgen.ads.NextGenAds

/**
 * Fullscreen Dialog that displays a Native Ad.
 * Used for the "Inter → Native Fullscreen → action" combo pattern.
 *
 * The native layout must be wrapped in a [NativeAdView] as the root element.
 * Optionally include a close button with id `@+id/ivClose` to let users dismiss.
 */
class NativeFullScreenDialog(
  private val activity: Activity,
  private val nativeAd: NativeAd,
  @param:LayoutRes private val layoutResId: Int,
  private val onDismiss: () -> Unit,
) : Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
    window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT
    )

    // Prevent dismiss on back press - user must tap close button
    setCancelable(false)
    setCanceledOnTouchOutside(false)

    try {
      val inflater = LayoutInflater.from(activity)
      val adView = inflater.inflate(layoutResId, null, false) as NativeAdView

      // Auto-bind views and populate ad data
      NativeAdHelper.autoBindViews(adView)
      NativeAdHelper.populateNativeAdView(nativeAd, adView)

      setContentView(adView)

      // Setup close button
      val closeId = activity.resources.getIdentifier("ivClose", "id", activity.packageName)
      val closeBtn = if (closeId != 0) adView.findViewById<View>(closeId) else null

      if (closeBtn != null) {
        closeBtn.visibility = View.VISIBLE
        closeBtn.setOnClickListener {
          dismissAndCallback()
        }
      } else {
        // If no close button in layout, add a default one
        val defaultClose = ImageView(activity).apply {
          setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
          setPadding(24, 24, 24, 24)
          setColorFilter(Color.WHITE)
        }
        val params = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.WRAP_CONTENT,
          FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          gravity = android.view.Gravity.TOP or android.view.Gravity.END
          topMargin = 48
          marginEnd = 24
        }

        if (adView is ViewGroup) {
          adView.addView(defaultClose, params)
        }
        defaultClose.setOnClickListener {
          dismissAndCallback()
        }
      }

      NextGenAds.log("NativeFullScreenDialog: showing native fullscreen ad")
    } catch (e: Exception) {
      NextGenAds.logError("NativeFullScreenDialog: failed to inflate layout: ${e.message}", e)
      dismissAndCallback()
    }
  }

  private fun dismissAndCallback() {
    try {
      nativeAd.destroy()
    } catch (_: Exception) {}
    try {
      dismiss()
    } catch (_: Exception) {}
    onDismiss()
  }

  @Suppress("DEPRECATION")
  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    // Block back press, user must tap close button
  }
}
