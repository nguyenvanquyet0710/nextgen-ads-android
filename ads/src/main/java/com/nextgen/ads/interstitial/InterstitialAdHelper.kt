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

package com.nextgen.ads.interstitial

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.callbacks.AdEventListener
import com.nextgen.ads.callbacks.AdPreloadListener

/**
 * Utility helper for loading, preloading, and showing Interstitial Ads in Next-Gen GMA SDK.
 */
object InterstitialAdHelper {

  // --- Preloading API ---

  /**
   * Starts preloading Interstitial Ads for the given [adUnitId].
   */
  fun startPreloader(
    adUnitId: String,
    preloadConfig: PreloadConfiguration? = null,
    listener: AdPreloadListener? = null,
  ) {
    val config = preloadConfig ?: PreloadConfiguration(AdRequest.Builder(adUnitId).build())

    InterstitialAdPreloader.start(
      adUnitId,
      config,
      object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
          NextGenAds.log("Interstitial ad preloaded for id: $preloadId")
          listener?.onAdPreloaded(preloadId, responseInfo)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
          NextGenAds.logError("Interstitial ad failed to preload ($preloadId): ${adError.message}")
          listener?.onAdFailedToPreload(preloadId, adError)
        }

        override fun onAdsExhausted(preloadId: String) {
          NextGenAds.log("Interstitial ads exhausted for id: $preloadId")
          listener?.onAdsExhausted(preloadId)
        }
      },
    )
  }

  /**
   * Checks whether a preloaded Interstitial Ad is available to show.
   */
  fun isPreloadedAdAvailable(adUnitId: String): Boolean {
    return InterstitialAdPreloader.isAdAvailable(adUnitId)
  }

  /**
   * Polls the next preloaded ad and shows it on the given [activity].
   *
   * @return true if an ad was found and presented, false otherwise.
   */
  fun pollAndShow(
    activity: Activity,
    adUnitId: String,
    callback: AdEventListener? = null,
  ): Boolean {
    val ad = InterstitialAdPreloader.pollAd(adUnitId) ?: return false
    show(activity, ad, callback)
    return true
  }

  // --- Single-Load API ---

  /**
   * Loads a single Interstitial Ad.
   */
  fun load(
    adUnitId: String,
    callback: (ad: InterstitialAd?, error: LoadAdError?) -> Unit,
  ) {
    val adRequest = AdRequest.Builder(adUnitId).build()
    InterstitialAd.load(
      adRequest,
      object : AdLoadCallback<InterstitialAd> {
        override fun onAdLoaded(ad: InterstitialAd) {
          NextGenAds.log("Interstitial ad loaded for: $adUnitId")
          callback(ad, null)
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
          NextGenAds.logError("Interstitial ad failed to load ($adUnitId): ${adError.message}")
          callback(null, adError)
        }
      },
    )
  }

  /**
   * Shows an [InterstitialAd] with full lifecycle event listeners.
   */
  fun show(
    activity: Activity,
    ad: InterstitialAd,
    callback: AdEventListener? = null,
  ) {
    ad.adEventCallback = object : InterstitialAdEventCallback {
      override fun onAdShowedFullScreenContent() {
        NextGenAds.log("Interstitial ad shown.")
        callback?.onAdShowed()
      }

      override fun onAdDismissedFullScreenContent() {
        NextGenAds.log("Interstitial ad dismissed.")
        callback?.onAdDismissed()
      }

      override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
        NextGenAds.logError("Interstitial ad failed to show: ${fullScreenContentError.message}")
        callback?.onAdFailedToShow(fullScreenContentError)
      }

      override fun onAdImpression() {
        NextGenAds.log("Interstitial ad recorded impression.")
        callback?.onAdImpression()
      }

      override fun onAdClicked() {
        NextGenAds.log("Interstitial ad clicked.")
        callback?.onAdClicked()
      }

      override fun onAdPaid(value: AdValue) {
        NextGenAds.log("Interstitial ad paid event: ${value.valueMicros} ${value.currencyCode}")
        callback?.onAdPaid(value)
      }
    }

    ad.show(activity)
  }
}
