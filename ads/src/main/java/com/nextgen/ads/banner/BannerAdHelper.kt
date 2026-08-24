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

package com.nextgen.ads.banner

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.nextgen.ads.AdFormat
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.callbacks.AdEventListener
import com.nextgen.ads.shimmer.AdShimmerView

/**
 * Utility helper for managing Banner Ads in Next-Gen GMA SDK.
 */
object BannerAdHelper {

  /**
   * Calculates the screen width in DP.
   */
  fun getScreenWidthDp(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    val adWidthPixels = displayMetrics.widthPixels
    val density = displayMetrics.density
    return (adWidthPixels / density).toInt()
  }

  /**
   * Returns anchored adaptive banner ad size.
   */
  fun getAdaptiveBannerAdSize(context: Context, widthDp: Int? = null): AdSize {
    val width = widthDp ?: getScreenWidthDp(context)
    return AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, width)
  }

  /**
   * Returns large anchored adaptive banner ad size.
   */
  fun getLargeAnchoredAdaptiveBannerAdSize(context: Context, widthDp: Int? = null): AdSize {
    val width = widthDp ?: getScreenWidthDp(context)
    return AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, width)
  }

  /**
   * Returns inline adaptive banner ad size.
   */
  fun getInlineAdaptiveBannerAdSize(context: Context, widthDp: Int? = null, maxHeightDp: Int = 0): AdSize {
    val width = widthDp ?: getScreenWidthDp(context)
    return if (maxHeightDp > 0) {
      AdSize.getInlineAdaptiveBannerAdSize(width, maxHeightDp)
    } else {
      AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, width)
    }
  }

  /**
   * Loads an adaptive banner ad into an [AdView].
   */
  fun loadAdaptiveBanner(
    adView: AdView,
    adUnitId: String,
    lifecycleOwner: LifecycleOwner? = null,
    isLarge: Boolean = false,
    callback: AdEventListener? = null,
  ) {
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.BANNER)
    val context = adView.context
    val adSize = if (isLarge) {
      getLargeAnchoredAdaptiveBannerAdSize(context)
    } else {
      getAdaptiveBannerAdSize(context)
    }
    loadBanner(adView, resolvedAdUnitId, adSize, lifecycleOwner, callback)
  }

  /**
   * Loads a banner ad into an [AdView] with a specified [AdSize].
   */
  fun loadBanner(
    adView: AdView,
    adUnitId: String,
    adSize: AdSize,
    lifecycleOwner: LifecycleOwner? = null,
    callback: AdEventListener? = null,
  ) {
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.BANNER)
    lifecycleOwner?.let { bindLifecycle(adView, it) }

    try {
      val bannerAdRequest = BannerAdRequest.Builder(resolvedAdUnitId, adSize).build()
      adView.loadAd(
        bannerAdRequest,
        object : AdLoadCallback<BannerAd> {
          override fun onAdLoaded(ad: BannerAd) {
            NextGenAds.log("Banner ad loaded for unit: $resolvedAdUnitId")

            ad.adEventCallback = object : BannerAdEventCallback {
              override fun onAdImpression() {
                NextGenAds.log("Banner ad impression recorded.")
                NextGenAds.runOnMainThread { callback?.onAdImpression() }
              }

              override fun onAdClicked() {
                NextGenAds.log("Banner ad clicked.")
                NextGenAds.runOnMainThread { callback?.onAdClicked() }
              }
            }

            ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
              override fun onAdRefreshed() {
                NextGenAds.log("Banner ad refreshed.")
                NextGenAds.runOnMainThread { callback?.onAdRefreshed() }
              }

              override fun onAdFailedToRefresh(adError: LoadAdError) {
                NextGenAds.logError("Banner ad failed to refresh: ${adError.message}")
                NextGenAds.runOnMainThread { callback?.onAdFailedToRefresh(adError) }
              }
            }

            NextGenAds.runOnMainThread { callback?.onAdLoaded() }
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            NextGenAds.logError("Banner ad failed to load: ${adError.message}")
            NextGenAds.runOnMainThread { callback?.onAdFailedToLoad(adError) }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to load Banner ad: ${e.message}", e)
    }
  }

  /**
   * Binds [AdView] destruction to the given [LifecycleOwner].
   */
  fun bindLifecycle(adView: AdView, lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onDestroy(owner: LifecycleOwner) {
        try {
          adView.destroy()
        } catch (e: Exception) {
          NextGenAds.logError("Error destroying AdView: ${e.message}", e)
        }
        owner.lifecycle.removeObserver(this)
      }
    })
  }

  /**
   * Loads and displays a Banner Ad into a plain [FrameLayout].
   * Automatically creates AdView, shows shimmer loading, and crossfades to the real ad.
   *
   * Usage in XML: just use `<FrameLayout android:id="@+id/fl_banner" .../>`
   * Usage in Kotlin: `BannerAdHelper.loadInto(binding.flBanner, "ad-unit-id", this)`
   *
   * @param container The FrameLayout on your layout.
   * @param adUnitId The AdMob Banner Ad Unit ID.
   * @param lifecycleOwner Automatically destroys AdView when Activity/Fragment is destroyed.
   * @param showShimmer Show shimmer loading placeholder while ad is loading (default: true).
   * @param shimmerLayoutResId Custom shimmer layout resource (null = use default banner shimmer).
   * @param isLarge Use large anchored adaptive banner size (default: false).
   * @param callback Ad lifecycle event listener.
   */
  fun loadInto(
    container: FrameLayout,
    adUnitId: String,
    lifecycleOwner: LifecycleOwner? = null,
    showShimmer: Boolean = true,
    @LayoutRes shimmerLayoutResId: Int? = null,
    isLarge: Boolean = false,
    callback: AdEventListener? = null,
  ) {
    // 1. Check if ads are enabled
    if (!NextGenAds.adConfig.isAdsEnabled) {
      container.visibility = View.GONE
      return
    }

    // 2. Resolve test/live ad unit ID
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.BANNER)
    NextGenAds.log("BannerAdHelper.loadInto: resolved=$resolvedAdUnitId (test=${NextGenAds.adConfig.isTestMode})")

    // 3. Show shimmer loading placeholder
    container.removeAllViews()
    if (showShimmer) {
      if (shimmerLayoutResId != null) {
        AdShimmerView.showCustomShimmer(container, shimmerLayoutResId)
      } else {
        AdShimmerView.showBannerShimmer(container)
      }
    }
    container.visibility = View.VISIBLE

    // 4. Create AdView programmatically
    val context = container.context
    val adView = AdView(context)
    adView.layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    )

    // 5. Bind lifecycle
    lifecycleOwner?.let { bindLifecycle(adView, it) }

    // 6. Calculate ad size
    val adSize = if (isLarge) {
      getLargeAnchoredAdaptiveBannerAdSize(context)
    } else {
      getAdaptiveBannerAdSize(context)
    }

    // 7. Load banner
    try {
      val bannerAdRequest = BannerAdRequest.Builder(resolvedAdUnitId, adSize).build()
      adView.loadAd(
        bannerAdRequest,
        object : AdLoadCallback<BannerAd> {
          override fun onAdLoaded(ad: BannerAd) {
            NextGenAds.log("Banner ad loaded into FrameLayout for: $resolvedAdUnitId")

            ad.adEventCallback = object : BannerAdEventCallback {
              override fun onAdImpression() {
                NextGenAds.log("Banner ad impression recorded.")
                NextGenAds.runOnMainThread { callback?.onAdImpression() }
              }

              override fun onAdClicked() {
                NextGenAds.log("Banner ad clicked.")
                NextGenAds.runOnMainThread { callback?.onAdClicked() }
              }
            }

            ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
              override fun onAdRefreshed() {
                NextGenAds.log("Banner ad refreshed.")
                NextGenAds.runOnMainThread { callback?.onAdRefreshed() }
              }

              override fun onAdFailedToRefresh(adError: LoadAdError) {
                NextGenAds.logError("Banner ad failed to refresh: ${adError.message}")
                NextGenAds.runOnMainThread { callback?.onAdFailedToRefresh(adError) }
              }
            }

            // Replace shimmer with the real ad (crossfade animation)
            NextGenAds.runOnMainThread {
              AdShimmerView.replaceShimmerWithAd(container, adView)
              callback?.onAdLoaded()
            }
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            NextGenAds.logError("Banner ad failed to load into FrameLayout: ${adError.message}")
            NextGenAds.runOnMainThread {
              AdShimmerView.removeShimmerAndHide(container)
              callback?.onAdFailedToLoad(adError)
            }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to load Banner ad into FrameLayout: ${e.message}", e)
      AdShimmerView.removeShimmerAndHide(container)
    }
  }
}
