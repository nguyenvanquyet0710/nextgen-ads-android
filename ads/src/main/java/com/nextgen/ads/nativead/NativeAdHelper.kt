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

import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.callbacks.AdEventListener
import com.nextgen.ads.callbacks.AdPreloadListener

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
    val config = preloadConfig ?: run {
      val videoOptions = VideoOptions.Builder().setStartMuted(startMuted).build()
      val adRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
        .setVideoOptions(videoOptions)
        .build()
      PreloadConfiguration(adRequest)
    }

    try {
      NativeAdPreloader.start(
        adUnitId,
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
    val videoOptions = VideoOptions.Builder().setStartMuted(startMuted).build()
    val adRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
      .setVideoOptions(videoOptions)
      .build()

    try {
      NativeAdLoader.load(
        adRequest,
        object : NativeAdLoaderCallback {
          override fun onNativeAdLoaded(nativeAd: NativeAd) {
            NextGenAds.log("Native ad loaded for: $adUnitId")
            NextGenAds.runOnMainThread { callback(nativeAd, null) }
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            NextGenAds.logError("Native ad failed to load ($adUnitId): ${adError.message}")
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
}
