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

package com.nextgen.ads.rewarded

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.callbacks.AdEventListener
import com.nextgen.ads.callbacks.AdPreloadListener

/**
 * Utility helper for loading, preloading, and presenting Rewarded Ads in Next-Gen GMA SDK.
 */
object RewardedAdHelper {

  // --- Preloader API ---

  /**
   * Starts preloading Rewarded Ads for the specified [adUnitId].
   */
  fun startPreloader(
    adUnitId: String,
    preloadConfig: PreloadConfiguration? = null,
    listener: AdPreloadListener? = null,
  ) {
    val config = preloadConfig ?: PreloadConfiguration(AdRequest.Builder(adUnitId).build())

    RewardedAdPreloader.start(
      adUnitId,
      config,
      object : PreloadCallback {
        override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
          NextGenAds.log("Rewarded ad preloaded for id: $preloadId")
          listener?.onAdPreloaded(preloadId, responseInfo)
        }

        override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
          NextGenAds.logError("Rewarded ad failed to preload ($preloadId): ${adError.message}")
          listener?.onAdFailedToPreload(preloadId, adError)
        }

        override fun onAdsExhausted(preloadId: String) {
          NextGenAds.log("Rewarded ads exhausted for id: $preloadId")
          listener?.onAdsExhausted(preloadId)
        }
      },
    )
  }

  /**
   * Checks whether a preloaded Rewarded Ad is available to show.
   */
  fun isPreloadedAdAvailable(adUnitId: String): Boolean {
    return RewardedAdPreloader.isAdAvailable(adUnitId)
  }

  /**
   * Polls the next preloaded ad and presents it on the given [activity].
   *
   * @return true if an ad was found and presented, false otherwise.
   */
  fun pollAndShow(
    activity: Activity,
    adUnitId: String,
    callback: AdEventListener? = null,
    onUserEarnedReward: (RewardItem) -> Unit,
  ): Boolean {
    val ad = RewardedAdPreloader.pollAd(adUnitId) ?: return false
    show(activity, ad, callback, onUserEarnedReward)
    return true
  }

  // --- Single-Load API ---

  /**
   * Loads a single Rewarded Ad.
   */
  fun load(
    adUnitId: String,
    callback: (ad: RewardedAd?, error: LoadAdError?) -> Unit,
  ) {
    val adRequest = AdRequest.Builder(adUnitId).build()
    RewardedAd.load(
      adRequest,
      object : AdLoadCallback<RewardedAd> {
        override fun onAdLoaded(ad: RewardedAd) {
          NextGenAds.log("Rewarded ad loaded for: $adUnitId")
          callback(ad, null)
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
          NextGenAds.logError("Rewarded ad failed to load ($adUnitId): ${adError.message}")
          callback(null, adError)
        }
      },
    )
  }

  /**
   * Shows a [RewardedAd] with lifecycle callbacks and reward listener.
   */
  fun show(
    activity: Activity,
    ad: RewardedAd,
    callback: AdEventListener? = null,
    onUserEarnedReward: (RewardItem) -> Unit,
  ) {
    ad.adEventCallback = object : RewardedAdEventCallback {
      override fun onAdShowedFullScreenContent() {
        NextGenAds.log("Rewarded ad shown.")
        callback?.onAdShowed()
      }

      override fun onAdDismissedFullScreenContent() {
        NextGenAds.log("Rewarded ad dismissed.")
        callback?.onAdDismissed()
      }

      override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
        NextGenAds.logError("Rewarded ad failed to show: ${fullScreenContentError.message}")
        callback?.onAdFailedToShow(fullScreenContentError)
      }

      override fun onAdImpression() {
        NextGenAds.log("Rewarded ad recorded impression.")
        callback?.onAdImpression()
      }

      override fun onAdClicked() {
        NextGenAds.log("Rewarded ad clicked.")
        callback?.onAdClicked()
      }

      override fun onAdPaid(value: AdValue) {
        NextGenAds.log("Rewarded ad paid event: ${value.valueMicros} ${value.currencyCode}")
        callback?.onAdPaid(value)
      }
    }

    ad.show(
      activity,
      OnUserEarnedRewardListener { rewardItem ->
        NextGenAds.log("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        onUserEarnedReward(rewardItem)
      },
    )
  }
}
