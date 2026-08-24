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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.nextgen.ads.AdFormat
import com.nextgen.ads.NextGenAds

import com.nextgen.ads.callbacks.AdEventListener
import com.nextgen.ads.callbacks.AdPreloadListener
import com.nextgen.ads.shimmer.AdShimmerView

/**
 * Utility helper for loading, preloading, and handling Native Ads in Next-Gen GMA SDK.
 */
object NativeAdHelper {

  // --- Preloader API ---

  /**
   * Starts preloading Native Ads for [adUnitId].
   */
  fun startPreloader(
    adUnitId: String,
    startMuted: Boolean = true,
    preloadConfig: PreloadConfiguration? = null,
    listener: AdPreloadListener? = null,
  ) {
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.NATIVE)
    val config = preloadConfig ?: run {
      val videoOptions = VideoOptions.Builder().setStartMuted(startMuted).build()
      val adRequest = NativeAdRequest.Builder(resolvedAdUnitId, listOf(NativeAd.NativeAdType.NATIVE))
        .setVideoOptions(videoOptions)
        .build()
      PreloadConfiguration(adRequest)
    }

    try {
      NativeAdPreloader.start(
        resolvedAdUnitId,
        config,
        object : PreloadCallback {
          override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
            NextGenAds.log("Native ad preloaded for id: $preloadId")
            NextGenAds.runOnMainThread { listener?.onAdPreloaded(preloadId, responseInfo) }
          }

          override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            NextGenAds.logError("Native ad failed to preload ($preloadId): ${adError.message}")
            NextGenAds.runOnMainThread { listener?.onAdFailedToPreload(preloadId, adError) }
          }

          override fun onAdsExhausted(preloadId: String) {
            NextGenAds.log("Native ads exhausted for id: $preloadId")
            NextGenAds.runOnMainThread { listener?.onAdsExhausted(preloadId) }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to start Native Preloader: ${e.message}", e)
    }
  }

  /**
   * Checks whether a preloaded Native Ad is available.
   */
  fun isPreloadedAdAvailable(adUnitId: String): Boolean {
    return try {
      NativeAdPreloader.isAdAvailable(adUnitId)
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Polls the next preloaded Native Ad.
   */
  fun pollAd(adUnitId: String): NativeAd? {
    val result = try {
      NativeAdPreloader.pollAd(adUnitId)
    } catch (e: Exception) {
      null
    }
    return if (result is NativeAdLoadResult.NativeAdSuccess) {
      result.ad
    } else {
      null
    }
  }

  /**
   * Destroys preloader resources for [adUnitId].
   */
  fun destroyPreloader(adUnitId: String) {
    try {
      NativeAdPreloader.destroy(adUnitId)
    } catch (e: Exception) {
      NextGenAds.logError("Error destroying Native Preloader: ${e.message}", e)
    }
  }

  // --- Single-Load API ---

  /**
   * Loads a single Native Ad.
   */
  fun load(
    adUnitId: String,
    startMuted: Boolean = true,
    callback: (ad: NativeAd?, error: LoadAdError?) -> Unit,
  ) {
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.NATIVE)
    val videoOptions = VideoOptions.Builder().setStartMuted(startMuted).build()
    val adRequest = NativeAdRequest.Builder(resolvedAdUnitId, listOf(NativeAd.NativeAdType.NATIVE))
      .setVideoOptions(videoOptions)
      .build()

    try {
      NativeAdLoader.load(
        adRequest,
        object : NativeAdLoaderCallback {
          override fun onNativeAdLoaded(nativeAd: NativeAd) {
            NextGenAds.log("Native ad loaded for: $resolvedAdUnitId")
            NextGenAds.runOnMainThread { callback(nativeAd, null) }
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            NextGenAds.logError("Native ad failed to load ($resolvedAdUnitId): ${adError.message}")
            NextGenAds.runOnMainThread { callback(null, adError) }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to load Native ad: ${e.message}", e)
    }
  }

  /**
   * Configures lifecycle event callback on a [NativeAd].
   */
  fun bindEventCallback(nativeAd: NativeAd, callback: AdEventListener) {
    nativeAd.adEventCallback = object : NativeAdEventCallback {
      override fun onAdShowedFullScreenContent() {
        NextGenAds.log("Native ad showed full screen content.")
        NextGenAds.runOnMainThread { callback.onAdShowed() }
      }

      override fun onAdDismissedFullScreenContent() {
        NextGenAds.log("Native ad dismissed full screen content.")
        NextGenAds.runOnMainThread { callback.onAdDismissed() }
      }

      override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
        NextGenAds.logError("Native ad failed to show full screen content: ${fullScreenContentError.message}")
        NextGenAds.runOnMainThread { callback.onAdFailedToShow(fullScreenContentError) }
      }

      override fun onAdImpression() {
        NextGenAds.log("Native ad recorded impression.")
        NextGenAds.runOnMainThread { callback.onAdImpression() }
      }

      override fun onAdClicked() {
        NextGenAds.log("Native ad clicked.")
        NextGenAds.runOnMainThread { callback.onAdClicked() }
      }

      override fun onAdPaid(value: AdValue) {
        NextGenAds.log("Native ad paid event: ${value.valueMicros} ${value.currencyCode}")
        NextGenAds.runOnMainThread { callback.onAdPaid(value) }
      }
    }
  }

  /**
   * Automatically populates a [NativeAdView] with data from a [NativeAd].
   * Handles visibility of views automatically (hides view if asset is null/empty).
   */
  fun populateNativeAdView(
    nativeAd: NativeAd,
    nativeAdView: com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView,
  ) {
    // 1. Headline
    (nativeAdView.headlineView as? android.widget.TextView)?.apply {
      text = nativeAd.headline
      visibility = if (nativeAd.headline.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    // 2. Body
    (nativeAdView.bodyView as? android.widget.TextView)?.apply {
      text = nativeAd.body
      visibility = if (nativeAd.body.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    // 3. Call To Action
    (nativeAdView.callToActionView as? android.widget.TextView)?.apply {
      text = nativeAd.callToAction
      visibility = if (nativeAd.callToAction.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    // 4. Icon
    (nativeAdView.iconView as? android.widget.ImageView)?.apply {
      val icon = nativeAd.icon
      if (icon?.drawable != null) {
        setImageDrawable(icon.drawable)
        visibility = android.view.View.VISIBLE
      } else if (icon?.uri != null) {
        setImageURI(icon.uri)
        visibility = android.view.View.VISIBLE
      } else {
        visibility = android.view.View.GONE
      }
    }

    // 5. Star Rating
    (nativeAdView.starRatingView as? android.widget.RatingBar)?.apply {
      val rating = nativeAd.starRating
      if (rating != null && rating > 0) {
        this.rating = rating.toFloat()
        visibility = android.view.View.VISIBLE
      } else {
        visibility = android.view.View.GONE
      }
    }

    // 6. Advertiser
    (nativeAdView.advertiserView as? android.widget.TextView)?.apply {
      text = nativeAd.advertiser
      visibility = if (nativeAd.advertiser.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    // 7. Store
    (nativeAdView.storeView as? android.widget.TextView)?.apply {
      text = nativeAd.store
      visibility = if (nativeAd.store.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    // 8. Price
    (nativeAdView.priceView as? android.widget.TextView)?.apply {
      text = nativeAd.price
      visibility = if (nativeAd.price.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }

    // 9. Bind & Register the NativeAd object to the NativeAdView container
    val mediaView = nativeAdView.mediaView
    if (mediaView != null) {
      mediaView.post {
        nativeAdView.registerNativeAd(nativeAd, mediaView)
      }
    } else {
      nativeAdView.registerNativeAd(nativeAd, null)
    }
  }

  // --- FrameLayout API (v1.1.0) ---

  /**
   * Loads and displays a Native Ad into a plain [FrameLayout].
   * Automatically inflates the custom layout, shows shimmer loading,
   * populates ad data, and crossfades to the real ad.
   *
   * Usage in XML: `<FrameLayout android:id="@+id/fl_native" .../>`
   * Usage in Kotlin:
   * ```
   * NativeAdHelper.loadInto(
   *     container = binding.flNative,
   *     adUnitId = "ca-app-pub-xxx/xxx",
   *     layoutResId = R.layout.ad_template_medium,
   *     lifecycleOwner = this
   * )
   * ```
   *
   * @param container The FrameLayout on your layout.
   * @param adUnitId The AdMob Native Ad Unit ID.
   * @param layoutResId Resource ID of your custom native ad layout XML (root must be NativeAdView).
   * @param lifecycleOwner Automatically destroys native ad when Activity/Fragment is destroyed.
   * @param showShimmer Show shimmer loading placeholder while ad is loading (default: true).
   * @param shimmerLayoutResId Custom shimmer layout resource (null = use default native medium shimmer).
   * @param startMuted Whether native ad video starts muted (default: true).
   * @param callback Ad lifecycle event listener.
   */
  fun loadInto(
    container: FrameLayout,
    adUnitId: String,
    @LayoutRes layoutResId: Int,
    lifecycleOwner: LifecycleOwner? = null,
    showShimmer: Boolean = true,
    @LayoutRes shimmerLayoutResId: Int? = null,
    startMuted: Boolean = true,
    callback: AdEventListener? = null,
  ) {
    // 1. Check if ads are enabled
    if (!NextGenAds.adConfig.isAdsEnabled) {
      container.visibility = View.GONE
      return
    }

    // 2. Resolve test/live ad unit ID
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.NATIVE)
    NextGenAds.log("NativeAdHelper.loadInto: resolved=$resolvedAdUnitId (test=${NextGenAds.adConfig.isTestMode})")

    // 3. Show shimmer loading placeholder
    container.removeAllViews()
    if (showShimmer) {
      if (shimmerLayoutResId != null) {
        AdShimmerView.showCustomShimmer(container, shimmerLayoutResId)
      } else {
        AdShimmerView.showNativeMediumShimmer(container)
      }
    }
    container.visibility = View.VISIBLE

    // 4. Load native ad
    load(resolvedAdUnitId, startMuted) { nativeAd, error ->
      if (nativeAd != null) {
        NextGenAds.runOnMainThread {
          try {
            // 5. Inflate custom layout
            val inflater = LayoutInflater.from(container.context)
            val adView = inflater.inflate(layoutResId, container, false) as NativeAdView

            // 6. Auto-discover and assign views by standard IDs
            autoBindViews(adView)

            // 7. Populate ad data
            populateNativeAdView(nativeAd, adView)

            // 8. Bind event callbacks
            callback?.let { bindEventCallback(nativeAd, it) }

            // 9. Bind lifecycle for cleanup
            lifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
              override fun onDestroy(owner: LifecycleOwner) {
                try {
                  nativeAd.destroy()
                } catch (_: Exception) {}
                owner.lifecycle.removeObserver(this)
              }
            })

            // 10. Replace shimmer with the real ad (crossfade)
            AdShimmerView.replaceShimmerWithAd(container, adView)
            callback?.onAdLoaded()
          } catch (e: Exception) {
            NextGenAds.logError("Failed to inflate native ad layout: ${e.message}", e)
            AdShimmerView.removeShimmerAndHide(container)
          }
        }
      } else {
        NextGenAds.runOnMainThread {
          AdShimmerView.removeShimmerAndHide(container)
          error?.let { callback?.onAdFailedToLoad(it) }
        }
      }
    }
  }

  /**
   * Loads a Native Ad in collapsible format into a plain [FrameLayout].
   * Initially shows expanded (Medium), then collapses to banner size when user taps close button.
   *
   * The layout must contain a close button with id `@+id/ivClose` and
   * a MediaView with id `@+id/ad_media` for the collapsible behavior.
   *
   * @param container The FrameLayout on your layout.
   * @param adUnitId The AdMob Native Ad Unit ID.
   * @param layoutResId Resource ID of your custom collapsible native ad layout XML.
   * @param collapsedHeightDp Height in dp after collapsing (default: 60dp).
   * @param lifecycleOwner Automatically destroys native ad when Activity/Fragment is destroyed.
   * @param showShimmer Show shimmer loading placeholder while ad is loading.
   * @param shimmerLayoutResId Custom shimmer layout resource.
   * @param callback Ad lifecycle event listener.
   */
  fun loadCollapsibleInto(
    container: FrameLayout,
    adUnitId: String,
    @LayoutRes layoutResId: Int,
    collapsedHeightDp: Int = 60,
    lifecycleOwner: LifecycleOwner? = null,
    showShimmer: Boolean = true,
    @LayoutRes shimmerLayoutResId: Int? = null,
    callback: AdEventListener? = null,
  ) {
    // 1. Check if ads are enabled
    if (!NextGenAds.adConfig.isAdsEnabled) {
      container.visibility = View.GONE
      return
    }

    // 2. Resolve test/live ad unit ID
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.NATIVE)
    NextGenAds.log("NativeAdHelper.loadCollapsibleInto: resolved=$resolvedAdUnitId")

    // 3. Show shimmer
    container.removeAllViews()
    if (showShimmer) {
      if (shimmerLayoutResId != null) {
        AdShimmerView.showCustomShimmer(container, shimmerLayoutResId)
      } else {
        AdShimmerView.showNativeMediumShimmer(container)
      }
    }
    container.visibility = View.VISIBLE

    // 4. Load native ad
    load(resolvedAdUnitId) { nativeAd, error ->
      if (nativeAd != null) {
        NextGenAds.runOnMainThread {
          try {
            val inflater = LayoutInflater.from(container.context)
            val adView = inflater.inflate(layoutResId, container, false) as NativeAdView

            // Auto-discover and assign views
            autoBindViews(adView)
            populateNativeAdView(nativeAd, adView)
            callback?.let { bindEventCallback(nativeAd, it) }

            val ivCloseId = adView.context.resources.getIdentifier("ivClose", "id", adView.context.packageName)
            val adMediaId = adView.context.resources.getIdentifier("ad_media", "id", adView.context.packageName)
            val ivClose = if (ivCloseId != 0) adView.findViewById<View>(ivCloseId) else null
            val adMedia = if (adMediaId != 0) adView.findViewById<View>(adMediaId) else null

            ivClose?.setOnClickListener {
              try {
                android.transition.TransitionManager.beginDelayedTransition(container)
              } catch (_: Exception) {}

              // Collapse: hide media and close button, shrink to banner size
              adMedia?.visibility = View.GONE
              ivClose.visibility = View.GONE

              val density = container.context.resources.displayMetrics.density
              adView.layoutParams?.height = (collapsedHeightDp * density).toInt()
              adView.requestLayout()
            }

            // Lifecycle cleanup
            lifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
              override fun onDestroy(owner: LifecycleOwner) {
                try {
                  nativeAd.destroy()
                } catch (_: Exception) {}
                owner.lifecycle.removeObserver(this)
              }
            })

            // Replace shimmer with the real ad
            AdShimmerView.replaceShimmerWithAd(container, adView)
            callback?.onAdLoaded()
          } catch (e: Exception) {
            NextGenAds.logError("Failed to inflate collapsible native ad layout: ${e.message}", e)
            AdShimmerView.removeShimmerAndHide(container)
          }
        }
      } else {
        NextGenAds.runOnMainThread {
          AdShimmerView.removeShimmerAndHide(container)
          error?.let { callback?.onAdFailedToLoad(it) }
        }
      }
    }
  }

  /**
   * Auto-discovers and assigns standard ad views by their resource IDs.
   * This eliminates the need for developers to manually assign each view.
   *
   * Expected IDs in the layout:
   * - `@+id/ad_headline` (TextView)
   * - `@+id/ad_body` (TextView)
   * - `@+id/ad_call_to_action` (TextView/Button)
   * - `@+id/ad_app_icon` (ImageView)
   * - `@+id/ad_media` (MediaView)
   * - `@+id/ad_stars` (RatingBar)
   * - `@+id/ad_advertiser` (TextView)
   * - `@+id/ad_store` (TextView)
   * - `@+id/ad_price` (TextView)
   */
  private fun autoBindViews(adView: NativeAdView) {
    val res = adView.context.resources
    val pkg = adView.context.packageName

    fun findId(name: String): Int = res.getIdentifier(name, "id", pkg)

    findId("ad_headline").takeIf { it != 0 }?.let { adView.headlineView = adView.findViewById(it) }
    findId("ad_body").takeIf { it != 0 }?.let { adView.bodyView = adView.findViewById(it) }
    findId("ad_call_to_action").takeIf { it != 0 }?.let { adView.callToActionView = adView.findViewById(it) }
    findId("ad_app_icon").takeIf { it != 0 }?.let { adView.iconView = adView.findViewById(it) }
    // Note: mediaView is read-only (val) in Next-Gen SDK - NativeAdView auto-discovers MediaView children
    findId("ad_stars").takeIf { it != 0 }?.let { adView.starRatingView = adView.findViewById(it) }
    findId("ad_advertiser").takeIf { it != 0 }?.let { adView.advertiserView = adView.findViewById(it) }
    findId("ad_store").takeIf { it != 0 }?.let { adView.storeView = adView.findViewById(it) }
    findId("ad_price").takeIf { it != 0 }?.let { adView.priceView = adView.findViewById(it) }
  }
}
