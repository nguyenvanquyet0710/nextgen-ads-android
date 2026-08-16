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
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.callbacks.AdEventListener

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
    val context = adView.context
    val adSize = if (isLarge) {
      getLargeAnchoredAdaptiveBannerAdSize(context)
    } else {
      getAdaptiveBannerAdSize(context)
    }
    loadBanner(adView, adUnitId, adSize, lifecycleOwner, callback)
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
    lifecycleOwner?.let { bindLifecycle(adView, it) }

    val bannerAdRequest = BannerAdRequest.Builder(adUnitId, adSize).build()

    adView.loadAd(
      bannerAdRequest,
      object : AdLoadCallback<BannerAd> {
        override fun onAdLoaded(ad: BannerAd) {
          NextGenAds.log("Banner ad loaded for unit: $adUnitId")

          ad.adEventCallback = object : BannerAdEventCallback {
            override fun onAdImpression() {
              NextGenAds.log("Banner ad impression recorded.")
              callback?.onAdImpression()
            }

            override fun onAdClicked() {
              NextGenAds.log("Banner ad clicked.")
              callback?.onAdClicked()
            }
          }

          ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
            override fun onAdRefreshed() {
              NextGenAds.log("Banner ad refreshed.")
              callback?.onAdRefreshed()
            }

            override fun onAdFailedToRefresh(adError: LoadAdError) {
              NextGenAds.logError("Banner ad failed to refresh: ${adError.message}")
              callback?.onAdFailedToRefresh(adError)
            }
          }

          callback?.onAdLoaded()
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
          NextGenAds.logError("Banner ad failed to load: ${adError.message}")
          callback?.onAdFailedToLoad(adError)
        }
      },
    )
  }

  /**
   * Binds [AdView] destruction to the given [LifecycleOwner].
   */
  fun bindLifecycle(adView: AdView, lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onDestroy(owner: LifecycleOwner) {
        adView.destroy()
        owner.lifecycle.removeObserver(this)
      }
    })
  }
}
