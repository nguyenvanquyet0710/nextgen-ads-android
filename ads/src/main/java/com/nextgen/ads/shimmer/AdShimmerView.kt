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

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.facebook.shimmer.ShimmerFrameLayout
import com.nextgen.ads.R

/**
 * Utility for creating and managing shimmer loading placeholders for ads.
 * Shimmer provides a visual loading indicator while ads are being fetched,
 * preventing jarring pop-in effects.
 */
object AdShimmerView {

  private const val SHIMMER_TAG = "nextgen_shimmer"

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
   * Inflates [layoutResId] (your native ad template) as a skeleton shimmer so the
   * placeholder matches the real ad size and structure (small / medium / fullscreen / etc.).
   *
   * Text, image, media, and button views are converted into gray placeholders;
   * the root layout width/height from XML are preserved.
   */
  fun showNativeShimmerFromLayout(container: FrameLayout, @LayoutRes layoutResId: Int): View {
    container.visibility = View.VISIBLE
    val context = container.context
    val inflater = LayoutInflater.from(context)

    val skeleton = inflater.inflate(layoutResId, container, false)
    applySkeletonStyle(skeleton)

    val width = skeleton.layoutParams?.width ?: ViewGroup.LayoutParams.MATCH_PARENT
    val height = skeleton.layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT

    val shimmer = ShimmerFrameLayout(context).apply {
      layoutParams = FrameLayout.LayoutParams(width, height)
      tag = SHIMMER_TAG
      setShimmer(
        com.facebook.shimmer.Shimmer.AlphaHighlightBuilder()
          .setDuration(1200L)
          .setBaseAlpha(0.7f)
          .setHighlightAlpha(1f)
          .setAutoStart(true)
          .build()
      )
    }

    // Keep skeleton matching parent of ShimmerFrameLayout
    skeleton.layoutParams = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      if (height == ViewGroup.LayoutParams.MATCH_PARENT) {
        ViewGroup.LayoutParams.MATCH_PARENT
      } else {
        ViewGroup.LayoutParams.WRAP_CONTENT
      },
    )
    shimmer.addView(skeleton)
    shimmer.startShimmer()
    container.addView(shimmer)
    return shimmer
  }

  /**
   * Inflates and adds a shimmer layout into the container.
   */
  private fun showShimmer(container: FrameLayout, @LayoutRes layoutResId: Int): View {
    container.visibility = View.VISIBLE
    val inflater = LayoutInflater.from(container.context)
    val shimmerView = inflater.inflate(layoutResId, container, false)
    shimmerView.tag = SHIMMER_TAG
    container.addView(shimmerView)
    return shimmerView
  }

  /**
   * Turns the inflated native template into a non-interactive skeleton for shimmer.
   */
  private fun applySkeletonStyle(root: View) {
    root.isClickable = false
    root.isFocusable = false
    styleViewRecursive(root)
  }

  private fun styleViewRecursive(view: View) {
    when (view) {
      is RatingBar -> {
        // Keep reserved space so height matches the real template.
        view.visibility = View.INVISIBLE
      }
      is TextView -> {
        view.text = ""
        view.hint = null
        view.setTextColor(Color.TRANSPARENT)
        view.setHintTextColor(Color.TRANSPARENT)
        view.background = ContextCompat.getDrawable(view.context, placeholderFor(view))
        view.isClickable = false
        view.isFocusable = false
      }
      is ImageView -> {
        view.setImageDrawable(null)
        view.background = ContextCompat.getDrawable(view.context, R.drawable.nextgen_shimmer_placeholder)
        view.isClickable = false
      }
      is ViewGroup -> {
        view.isClickable = false
        for (i in 0 until view.childCount) {
          styleViewRecursive(view.getChildAt(i))
        }
      }
      else -> {
        if (looksLikeMediaView(view)) {
          view.background = ContextCompat.getDrawable(view.context, R.drawable.nextgen_shimmer_placeholder)
        }
        view.isClickable = false
      }
    }
  }

  private fun placeholderFor(view: TextView): Int {
    // CTA buttons usually look better with a rounder bar.
    val name = safeResourceEntryName(view)
    return if (name.contains("call_to_action") || name.contains("cta") || view is android.widget.Button) {
      R.drawable.nextgen_shimmer_placeholder_round
    } else {
      R.drawable.nextgen_shimmer_placeholder
    }
  }

  private fun looksLikeMediaView(view: View): Boolean {
    val simpleName = view.javaClass.simpleName
    if (simpleName.contains("MediaView", ignoreCase = true)) return true
    val name = safeResourceEntryName(view)
    return name.contains("media", ignoreCase = true)
  }

  private fun safeResourceEntryName(view: View): String {
    return try {
      if (view.id == View.NO_ID) "" else view.resources.getResourceEntryName(view.id)
    } catch (_: Exception) {
      ""
    }
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
    val shimmerView = container.findViewWithTag<View>(SHIMMER_TAG)
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
          (shimmerView as? ShimmerFrameLayout)?.stopShimmer()
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
    val shimmerView = container.findViewWithTag<View>(SHIMMER_TAG)
    if (shimmerView != null) {
      try {
        (shimmerView as? ShimmerFrameLayout)?.stopShimmer()
      } catch (_: Exception) {}
      container.removeView(shimmerView)
    }
    container.visibility = View.GONE
  }
}
