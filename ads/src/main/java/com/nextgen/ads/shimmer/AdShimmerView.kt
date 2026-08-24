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

package com.nextgen.ads.shimmer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.nextgen.ads.R

/**
 * Utility for creating and managing shimmer loading placeholders for ads.
 * Shimmer provides a visual loading indicator while ads are being fetched,
 * preventing jarring pop-in effects.
 */
object AdShimmerView {

  /**
   * Creates and adds a shimmer placeholder for Banner ads into the given [container].
   * The shimmer matches the approximate size of an adaptive banner (~60dp).
   *
   * @param container The FrameLayout to host the shimmer view.
   * @return The inflated shimmer View (so it can be removed later).
   */
  fun showBannerShimmer(container: FrameLayout): View {
    return showShimmer(container, R.layout.nextgen_shimmer_banner)
  }

  /**
   * Creates and adds a shimmer placeholder for Native Medium ads into the given [container].
   *
   * @param container The FrameLayout to host the shimmer view.
   * @return The inflated shimmer View.
   */
  fun showNativeMediumShimmer(container: FrameLayout): View {
    return showShimmer(container, R.layout.nextgen_shimmer_native_medium)
  }

  /**
   * Creates and adds a shimmer placeholder from a custom layout resource.
   *
   * @param container The FrameLayout to host the shimmer view.
   * @param layoutResId The layout resource ID for the custom shimmer layout.
   * @return The inflated shimmer View.
   */
  fun showCustomShimmer(container: FrameLayout, @LayoutRes layoutResId: Int): View {
    return showShimmer(container, layoutResId)
  }

  /**
   * Inflates and adds a shimmer layout into the container.
   */
  private fun showShimmer(container: FrameLayout, @LayoutRes layoutResId: Int): View {
    container.visibility = View.VISIBLE
    val inflater = LayoutInflater.from(container.context)
    val shimmerView = inflater.inflate(layoutResId, container, false)
    shimmerView.tag = "nextgen_shimmer"
    container.addView(shimmerView)
    return shimmerView
  }

  /**
   * Removes the shimmer view from the container with a smooth fade-out animation,
   * then adds the actual ad view with a fade-in animation.
   *
   * @param container The FrameLayout hosting the shimmer.
   * @param adView The actual ad view to display.
   * @param fadeDurationMs Duration of the crossfade animation in milliseconds.
   */
  fun replaceShimmerWithAd(container: FrameLayout, adView: View, fadeDurationMs: Long = 300L) {
    // Find and remove shimmer
    val shimmerView = container.findViewWithTag<View>("nextgen_shimmer")
    if (shimmerView != null) {
      val fadeOut = AlphaAnimation(1f, 0f).apply {
        duration = fadeDurationMs
        fillAfter = true
      }
      shimmerView.startAnimation(fadeOut)
      shimmerView.postDelayed({
        container.removeView(shimmerView)
        // Stop shimmer if it's a ShimmerFrameLayout
        try {
          (shimmerView as? com.facebook.shimmer.ShimmerFrameLayout)?.stopShimmer()
        } catch (_: Exception) {}
      }, fadeDurationMs)
    }

    // Add ad view with fade-in
    adView.alpha = 0f
    if (adView.parent != null) {
      (adView.parent as? ViewGroup)?.removeView(adView)
    }
    container.addView(adView)
    adView.animate()
      .alpha(1f)
      .setDuration(fadeDurationMs)
      .start()
  }

  /**
   * Removes the shimmer view from the container and hides it.
   * Used when ad loading fails.
   */
  fun removeShimmerAndHide(container: FrameLayout) {
    val shimmerView = container.findViewWithTag<View>("nextgen_shimmer")
    if (shimmerView != null) {
      try {
        (shimmerView as? com.facebook.shimmer.ShimmerFrameLayout)?.stopShimmer()
      } catch (_: Exception) {}
      container.removeView(shimmerView)
    }
    container.visibility = View.GONE
  }
}
