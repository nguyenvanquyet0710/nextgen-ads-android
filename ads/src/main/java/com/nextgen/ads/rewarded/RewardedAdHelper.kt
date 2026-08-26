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
import com.nextgen.ads.AdFormat
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
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.REWARDED)
    val config = preloadConfig ?: PreloadConfiguration(AdRequest.Builder(resolvedAdUnitId).build())

    try {
      RewardedAdPreloader.start(
        resolvedAdUnitId,
        config,
        object : PreloadCallback {
          override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
            NextGenAds.log("Rewarded ad preloaded for id: $preloadId")
            NextGenAds.runOnMainThread { listener?.onAdPreloaded(preloadId, responseInfo) }
          }

          override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            NextGenAds.logError("Rewarded ad failed to preload ($preloadId): ${adError.message}")
            NextGenAds.runOnMainThread { listener?.onAdFailedToPreload(preloadId, adError) }
          }

          override fun onAdsExhausted(preloadId: String) {
            NextGenAds.log("Rewarded ads exhausted for id: $preloadId")
            NextGenAds.runOnMainThread { listener?.onAdsExhausted(preloadId) }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to start Rewarded Preloader: ${e.message}", e)
    }
  }

  /**
   * Checks whether a preloaded Rewarded Ad is available to show.
   */
  fun isPreloadedAdAvailable(adUnitId: String): Boolean {
    return try {
      RewardedAdPreloader.isAdAvailable(adUnitId)
    } catch (e: Exception) {
      false
    }
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
    val ad = try {
      RewardedAdPreloader.pollAd(adUnitId)
    } catch (e: Exception) {
      null
    } ?: return false

    show(activity, ad, callback, onUserEarnedReward)
    return true
  }

  /**
   * Displays a loading overlay briefly before presenting the preloaded Rewarded Ad.
   */
  fun pollAndShowWithLoading(
    activity: Activity,
    adUnitId: String,
    loadingMessage: String = "Loading...",
    loadingDurationMs: Long = 800L,
    callback: AdEventListener? = null,
    onUserEarnedReward: (RewardItem) -> Unit,
    onComplete: (() -> Unit)? = null,
  ) {
    if (!NextGenAds.canShowAds(activity)) {
      onComplete?.invoke()
      return
    }
    NextGenAds.runOnMainThread {
      if (!isPreloadedAdAvailable(adUnitId)) {
        onComplete?.invoke()
        return@runOnMainThread
      }

      val dialog = com.nextgen.ads.dialogs.AdLoadingDialog(activity, loadingMessage)
      dialog.show()

      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        dialog.dismiss()
        val success = pollAndShow(
          activity = activity,
          adUnitId = adUnitId,
          callback = object : AdEventListener {
            override fun onAdDismissed() {
              callback?.onAdDismissed()
              onComplete?.invoke()
            }

            override fun onAdFailedToShow(error: FullScreenContentError) {
              callback?.onAdFailedToShow(error)
              onComplete?.invoke()
            }

            override fun onAdShowed() {
              callback?.onAdShowed()
            }

            override fun onAdImpression() {
              callback?.onAdImpression()
            }

            override fun onAdClicked() {
              callback?.onAdClicked()
            }

            override fun onAdPaid(value: AdValue) {
              callback?.onAdPaid(value)
            }
          },
          onUserEarnedReward = onUserEarnedReward,
        )
        if (!success) {
          onComplete?.invoke()
        }
      }, loadingDurationMs)
    }
  }

  // --- Single-Load API ---

  /**
   * Loads a single Rewarded Ad.
   */
  fun load(
    adUnitId: String,
    callback: (ad: RewardedAd?, error: LoadAdError?) -> Unit,
  ) {
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.REWARDED)
    val adRequest = AdRequest.Builder(resolvedAdUnitId).build()
    try {
      RewardedAd.load(
        adRequest,
        object : AdLoadCallback<RewardedAd> {
          override fun onAdLoaded(ad: RewardedAd) {
            NextGenAds.log("Rewarded ad loaded for: $adUnitId")
            NextGenAds.runOnMainThread { callback(ad, null) }
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            NextGenAds.logError("Rewarded ad failed to load ($adUnitId): ${adError.message}")
            NextGenAds.runOnMainThread { callback(null, adError) }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to load Rewarded Ad: ${e.message}", e)
    }
  }

  /**
   * Displays a loading overlay while loading and presenting a single Rewarded Ad.
   */
  fun loadAndShowWithLoading(
    activity: Activity,
    adUnitId: String,
    loadingMessage: String = "Loading...",
    callback: AdEventListener? = null,
    onUserEarnedReward: (RewardItem) -> Unit,
    onComplete: (() -> Unit)? = null,
  ) {
    if (!NextGenAds.canShowAds(activity)) {
      onComplete?.invoke()
      return
    }
    NextGenAds.runOnMainThread {
      val dialog = com.nextgen.ads.dialogs.AdLoadingDialog(activity, loadingMessage)
      dialog.show()

      load(adUnitId) { ad, error ->
        dialog.dismiss()
        if (ad != null) {
          show(
            activity,
            ad,
            object : AdEventListener {
              override fun onAdDismissed() {
                callback?.onAdDismissed()
                onComplete?.invoke()
              }

              override fun onAdFailedToShow(error: FullScreenContentError) {
                callback?.onAdFailedToShow(error)
                onComplete?.invoke()
              }

              override fun onAdShowed() {
                callback?.onAdShowed()
              }

              override fun onAdImpression() {
                callback?.onAdImpression()
              }

              override fun onAdClicked() {
                callback?.onAdClicked()
              }

              override fun onAdPaid(value: AdValue) {
                callback?.onAdPaid(value)
              }
            },
            onUserEarnedReward,
          )
        } else {
          onComplete?.invoke()
        }
      }
    }
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
    NextGenAds.runOnMainThread {
      ad.adEventCallback = object : RewardedAdEventCallback {
        override fun onAdShowedFullScreenContent() {
          NextGenAds.log("Rewarded ad shown.")
          NextGenAds.isFullScreenAdShowing = true
          NextGenAds.runOnMainThread { callback?.onAdShowed() }
        }

        override fun onAdDismissedFullScreenContent() {
          NextGenAds.log("Rewarded ad dismissed.")
          NextGenAds.isFullScreenAdShowing = false
          NextGenAds.runOnMainThread { callback?.onAdDismissed() }
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
          NextGenAds.logError("Rewarded ad failed to show: ${fullScreenContentError.message}")
          NextGenAds.isFullScreenAdShowing = false
          NextGenAds.runOnMainThread { callback?.onAdFailedToShow(fullScreenContentError) }
        }

        override fun onAdImpression() {
          NextGenAds.log("Rewarded ad recorded impression.")
          NextGenAds.runOnMainThread { callback?.onAdImpression() }
        }

        override fun onAdClicked() {
          NextGenAds.log("Rewarded ad clicked.")
          NextGenAds.runOnMainThread { callback?.onAdClicked() }
        }

        override fun onAdPaid(value: AdValue) {
          NextGenAds.log("Rewarded ad paid event: ${value.valueMicros} ${value.currencyCode}")
          NextGenAds.runOnMainThread { callback?.onAdPaid(value) }
        }
      }

      try {
        ad.show(
          activity,
          OnUserEarnedRewardListener { rewardItem ->
            NextGenAds.log("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            NextGenAds.runOnMainThread { onUserEarnedReward(rewardItem) }
          },
        )
      } catch (e: Exception) {
        NextGenAds.isFullScreenAdShowing = false
        NextGenAds.logError("Error showing Rewarded ad: ${e.message}", e)
      }
    }
  }
}
