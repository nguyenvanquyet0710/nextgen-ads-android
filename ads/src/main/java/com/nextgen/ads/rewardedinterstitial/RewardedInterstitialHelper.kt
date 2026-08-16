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

package com.nextgen.ads.rewardedinterstitial

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdPreloader
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.callbacks.AdEventListener
import com.nextgen.ads.callbacks.AdPreloadListener

/**
 * Utility helper for loading, preloading, and showing Rewarded Interstitial Ads in Next-Gen GMA SDK.
 */
object RewardedInterstitialHelper {

  // --- Preloader API ---

  /**
   * Starts preloading Rewarded Interstitial Ads for the specified [adUnitId].
   */
  fun startPreloader(
    adUnitId: String,
    preloadConfig: PreloadConfiguration? = null,
    listener: AdPreloadListener? = null,
  ) {
    val config = preloadConfig ?: PreloadConfiguration(AdRequest.Builder(adUnitId).build())

    RewardedInterstitialAdPreloader.start(
      adUnitId,
      config,
      object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
          NextGenAds.log("Rewarded Interstitial ad preloaded for id: $preloadId")
          listener?.onAdPreloaded(preloadId, responseInfo)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
          NextGenAds.logError("Rewarded Interstitial ad failed to preload ($preloadId): ${adError.message}")
          listener?.onAdFailedToPreload(preloadId, adError)
        }

        override fun onAdsExhausted(preloadId: String) {
          NextGenAds.log("Rewarded Interstitial ads exhausted for id: $preloadId")
          listener?.onAdsExhausted(preloadId)
        }
      },
    )
  }

  /**
   * Checks whether a preloaded Rewarded Interstitial Ad is available.
   */
  fun isPreloadedAdAvailable(adUnitId: String): Boolean {
    return RewardedInterstitialAdPreloader.isAdAvailable(adUnitId)
  }

  /**
   * Polls the next preloaded ad and shows it on the given [activity].
   */
  fun pollAndShow(
    activity: Activity,
    adUnitId: String,
    callback: AdEventListener? = null,
    onUserEarnedReward: (RewardItem) -> Unit,
  ): Boolean {
    val ad = RewardedInterstitialAdPreloader.pollAd(adUnitId) ?: return false
    show(activity, ad, callback, onUserEarnedReward)
    return true
  }

  /**
   * Destroys preloader resources for [adUnitId].
   */
  fun destroyPreloader(adUnitId: String) {
    RewardedInterstitialAdPreloader.destroy(adUnitId)
  }

  // --- Single-Load API ---

  /**
   * Loads a single Rewarded Interstitial Ad.
   */
  fun load(
    adUnitId: String,
    callback: (ad: RewardedInterstitialAd?, error: LoadAdError?) -> Unit,
  ) {
    val adRequest = AdRequest.Builder(adUnitId).build()
    RewardedInterstitialAd.load(
      adRequest,
      object : AdLoadCallback<RewardedInterstitialAd> {
        override fun onAdLoaded(ad: RewardedInterstitialAd) {
          NextGenAds.log("Rewarded Interstitial ad loaded for: $adUnitId")
          NextGenAds.runOnMainThread { callback(ad, null) }
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
          NextGenAds.logError("Rewarded Interstitial ad failed to load ($adUnitId): ${adError.message}")
          NextGenAds.runOnMainThread { callback(null, adError) }
        }
      },
    )
  }

  /**
   * Shows a [RewardedInterstitialAd].
   */
  fun show(
    activity: Activity,
    ad: RewardedInterstitialAd,
    callback: AdEventListener? = null,
    onUserEarnedReward: (RewardItem) -> Unit,
  ) {
    NextGenAds.runOnMainThread {
      ad.adEventCallback = object : RewardedInterstitialAdEventCallback {
        override fun onAdShowedFullScreenContent() {
          NextGenAds.log("Rewarded Interstitial ad shown.")
          NextGenAds.runOnMainThread { callback?.onAdShowed() }
        }

        override fun onAdDismissedFullScreenContent() {
          NextGenAds.log("Rewarded Interstitial ad dismissed.")
          NextGenAds.runOnMainThread { callback?.onAdDismissed() }
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
          NextGenAds.logError("Rewarded Interstitial ad failed to show: ${fullScreenContentError.message}")
          NextGenAds.runOnMainThread { callback?.onAdFailedToShow(fullScreenContentError) }
        }

        override fun onAdImpression() {
          NextGenAds.log("Rewarded Interstitial ad recorded impression.")
          NextGenAds.runOnMainThread { callback?.onAdImpression() }
        }

        override fun onAdClicked() {
          NextGenAds.log("Rewarded Interstitial ad clicked.")
          NextGenAds.runOnMainThread { callback?.onAdClicked() }
        }

        override fun onAdPaid(value: AdValue) {
          NextGenAds.log("Rewarded Interstitial ad paid event: ${value.valueMicros} ${value.currencyCode}")
          NextGenAds.runOnMainThread { callback?.onAdPaid(value) }
        }
      }

      try {
        ad.show(activity) { rewardItem ->
          NextGenAds.log("User earned reward from rewarded interstitial: ${rewardItem.amount} ${rewardItem.type}")
          NextGenAds.runOnMainThread { onUserEarnedReward(rewardItem) }
        }
      } catch (e: Exception) {
        NextGenAds.logError("Error showing Rewarded Interstitial ad: ${e.message}", e)
      }
    }
  }
}
