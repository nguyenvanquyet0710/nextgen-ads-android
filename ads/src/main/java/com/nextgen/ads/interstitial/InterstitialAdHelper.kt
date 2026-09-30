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
import com.nextgen.ads.AdFormat
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
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.INTERSTITIAL)
    val config = preloadConfig ?: PreloadConfiguration(AdRequest.Builder(resolvedAdUnitId).build())

    try {
      InterstitialAdPreloader.start(
        resolvedAdUnitId,
        config,
        object : PreloadCallback {
          override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
            NextGenAds.log("Interstitial ad preloaded for id: $preloadId")
            NextGenAds.runOnMainThread { listener?.onAdPreloaded(preloadId, responseInfo) }
          }

          override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
            NextGenAds.logError("Interstitial ad failed to preload ($preloadId): ${adError.message}")
            NextGenAds.runOnMainThread { listener?.onAdFailedToPreload(preloadId, adError) }
          }

          override fun onAdsExhausted(preloadId: String) {
            NextGenAds.log("Interstitial ads exhausted for id: $preloadId")
            NextGenAds.runOnMainThread { listener?.onAdsExhausted(preloadId) }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to start Interstitial Preloader. Ensure NextGenAds.initialize(...) is called: ${e.message}", e)
    }
  }

  /**
   * Checks whether a preloaded Interstitial Ad is available to show.
   */
  fun isPreloadedAdAvailable(adUnitId: String): Boolean {
    return try {
      InterstitialAdPreloader.isAdAvailable(adUnitId)
    } catch (e: Exception) {
      false
    }
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
    val ad = try {
      InterstitialAdPreloader.pollAd(adUnitId)
    } catch (e: Exception) {
      null
    } ?: return false

    show(activity, ad, callback)
    return true
  }

  /**
   * Displays a loading overlay briefly before presenting the preloaded Interstitial Ad.
   *
   * @param activity The calling activity.
   * @param adUnitId The AdMob Interstitial Ad Unit ID.
   * @param loadingMessage Message displayed in the loading dialog.
   * @param loadingDurationMs Duration to show the loading overlay (default: 800ms).
   * @param callback Ad lifecycle event listener.
   * @param onComplete Callback invoked when the ad is closed or if no ad is available.
   */
  fun pollAndShowWithLoading(
    activity: Activity,
    adUnitId: String,
    loadingMessage: String = "Loading...",
    loadingDurationMs: Long = 800L,
    callback: AdEventListener? = null,
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
          activity,
          adUnitId,
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
        )
        if (!success) {
          onComplete?.invoke()
        }
      }, loadingDurationMs)
    }
  }

  /**
   * Displays a loading overlay while loading and presenting a single Interstitial Ad.
   */
  fun loadAndShowWithLoading(
    activity: Activity,
    adUnitId: String,
    loadingMessage: String = "Loading...",
    callback: AdEventListener? = null,
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
          )
        } else {
          onComplete?.invoke()
        }
      }
    }
  }

  // --- Single-Load API ---

  /**
   * Loads a single Interstitial Ad.
   */
  fun load(
    adUnitId: String,
    callback: (ad: InterstitialAd?, error: LoadAdError?) -> Unit,
  ) {
    val resolvedAdUnitId = NextGenAds.resolveAdUnitId(adUnitId, AdFormat.INTERSTITIAL)
    val adRequest = AdRequest.Builder(resolvedAdUnitId).build()
    try {
      InterstitialAd.load(
        adRequest,
        object : AdLoadCallback<InterstitialAd> {
          override fun onAdLoaded(ad: InterstitialAd) {
            NextGenAds.log("Interstitial ad loaded for: $adUnitId")
            NextGenAds.runOnMainThread {
              callback(ad, null)
            }
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            NextGenAds.logError("Interstitial ad failed to load ($adUnitId): ${adError.message}")
            NextGenAds.runOnMainThread {
              callback(null, adError)
            }
          }
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Failed to load Interstitial Ad. Ensure NextGenAds.initialize(...) is called: ${e.message}", e)
    }
  }

  /**
   * Shows an [InterstitialAd] with full lifecycle event listeners.
   */
  fun show(
    activity: Activity,
    ad: InterstitialAd,
    callback: AdEventListener? = null,
  ) {
    NextGenAds.runOnMainThread {
      ad.adEventCallback = object : InterstitialAdEventCallback {
        override fun onAdShowedFullScreenContent() {
          NextGenAds.log("Interstitial ad shown.")
          NextGenAds.isFullScreenAdShowing = true
          NextGenAds.runOnMainThread { callback?.onAdShowed() }
        }

        override fun onAdDismissedFullScreenContent() {
          NextGenAds.log("Interstitial ad dismissed.")
          NextGenAds.isFullScreenAdShowing = false
          NextGenAds.runOnMainThread { callback?.onAdDismissed() }
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
          NextGenAds.logError("Interstitial ad failed to show: ${fullScreenContentError.message}")
          NextGenAds.isFullScreenAdShowing = false
          NextGenAds.runOnMainThread { callback?.onAdFailedToShow(fullScreenContentError) }
        }

        override fun onAdImpression() {
          NextGenAds.log("Interstitial ad recorded impression.")
          NextGenAds.runOnMainThread { callback?.onAdImpression() }
        }

        override fun onAdClicked() {
          NextGenAds.log("Interstitial ad clicked.")
          NextGenAds.runOnMainThread { callback?.onAdClicked() }
        }

        override fun onAdPaid(value: AdValue) {
          NextGenAds.log("Interstitial ad paid event: ${value.valueMicros} ${value.currencyCode}")
          NextGenAds.runOnMainThread { callback?.onAdPaid(value) }
        }
      }

      try {
        ad.show(activity)
      } catch (e: Exception) {
        NextGenAds.isFullScreenAdShowing = false
        NextGenAds.logError("Error showing Interstitial ad: ${e.message}", e)
      }
    }
  }

  /**
   * Combo: Load both Interstitial and Native Fullscreen in parallel with a loading dialog,
   * then show whichever ads are available. A loaded ad is never discarded because the other failed.
   *
   * Flow:
   * 1. Show Loading Dialog
   * 2. Load Interstitial & Native concurrently
   * 3. Interstitial loaded -> Dismiss dialog -> Show Interstitial immediately (does not wait for Native)
   *    - When Inter dismissed: Native ready -> Show Native Fullscreen, otherwise -> Execute onComplete
   * 4. Interstitial failed to load / show -> Wait for Native:
   *    - Native loaded -> Show Native Fullscreen alone
   *    - Native failed -> Execute onComplete
   * 5. When Native Fullscreen dismissed -> Execute onComplete
   *
   * Callers should navigate / finish the Activity inside [onComplete], not before calling this,
   * otherwise the ads cannot be shown.
   *
   * @param activity The current Activity.
   * @param interAdUnitId The Interstitial Ad Unit ID.
   * @param nativeAdUnitId The Native Ad Unit ID.
   * @param nativeLayoutResId Layout resource for the fullscreen native ad (default: library built-in layout).
   * @param loadingMessage Loading dialog message text.
   * @param onComplete Called exactly once after all shown ads are dismissed, or when no ad can be shown.
   */
  fun showThenNativeFullScreen(
    activity: Activity,
    interAdUnitId: String,
    nativeAdUnitId: String,
    @androidx.annotation.LayoutRes nativeLayoutResId: Int = com.nextgen.ads.R.layout.nextgen_native_fullscreen,
    loadingMessage: String = "Loading...",
    onComplete: () -> Unit,
  ) {
    if (!NextGenAds.canShowAds(activity)) {
      onComplete()
      return
    }

    NextGenAds.runOnMainThread {
      if (activity.isFinishing || activity.isDestroyed) {
        onComplete()
        return@runOnMainThread
      }

      val dialog = com.nextgen.ads.dialogs.AdLoadingDialog(activity, loadingMessage)
      dialog.show()

      var interState = ComboInterState.LOADING
      var nativeAd: com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd? = null
      var nativeFinished = false
      var hasCompleted = false

      fun isActivityGone() = activity.isFinishing || activity.isDestroyed

      fun complete() {
        if (hasCompleted) return
        hasCompleted = true
        dialog.dismiss()
        try {
          nativeAd?.destroy()
        } catch (_: Exception) {}
        nativeAd = null
        onComplete()
      }

      fun showNativeOrComplete() {
        if (hasCompleted) return
        val readyNative = nativeAd
        if (readyNative == null || isActivityGone()) {
          complete()
          return
        }
        hasCompleted = true
        nativeAd = null
        dialog.dismiss()
        com.nextgen.ads.nativead.NativeFullScreenActivity.launch(
          activity = activity,
          adUnitId = nativeAdUnitId,
          layoutResId = nativeLayoutResId,
          nativeAd = readyNative,
          onDismiss = { onComplete() },
        )
      }

      fun onInterUnavailable() {
        interState = ComboInterState.UNAVAILABLE
        if (nativeFinished) {
          showNativeOrComplete()
        }
      }

      // Load Interstitial
      load(interAdUnitId) { ad, error ->
        if (hasCompleted) return@load
        if (ad == null) {
          NextGenAds.logError("Combo: Interstitial failed to load (${error?.message})")
          onInterUnavailable()
          return@load
        }
        if (isActivityGone()) {
          complete()
          return@load
        }

        interState = ComboInterState.SHOWING
        dialog.dismiss()
        show(
          activity = activity,
          ad = ad,
          callback = object : AdEventListener {
            override fun onAdDismissed() {
              interState = ComboInterState.CLOSED
              showNativeOrComplete()
            }

            override fun onAdFailedToShow(error: FullScreenContentError) {
              NextGenAds.logError("Combo: Interstitial failed to show (${error.message})")
              if (!nativeFinished && !isActivityGone()) {
                // Keeps the screen covered (and App Open blocked) while the native is still loading.
                dialog.show()
              }
              onInterUnavailable()
            }
          },
        )
      }

      // Load Native
      com.nextgen.ads.nativead.NativeAdHelper.load(nativeAdUnitId) { ad, error ->
        if (hasCompleted) {
          try {
            ad?.destroy()
          } catch (_: Exception) {}
          return@load
        }
        nativeFinished = true
        nativeAd = ad
        if (ad == null) {
          NextGenAds.logError("Combo: Native failed to load (${error?.message})")
        }
        // While the interstitial is loading or on screen, the native waits for its dismissal.
        if (interState == ComboInterState.UNAVAILABLE) {
          showNativeOrComplete()
        }
      }
    }
  }

  private enum class ComboInterState { LOADING, SHOWING, CLOSED, UNAVAILABLE }
}
