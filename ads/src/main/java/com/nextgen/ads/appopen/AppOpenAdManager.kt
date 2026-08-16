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

package com.nextgen.ads.appopen

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.nextgen.ads.NextGenAds
import com.nextgen.ads.callbacks.AdEventListener
import com.nextgen.ads.callbacks.OnShowAdCompleteListener
import java.util.Date
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Singleton manager for loading and presenting App Open Ads in Next-Gen GMA SDK.
 */
object AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
  private var appOpenAd: AppOpenAd? = null
  private var isLoadingAd = false
  var isShowingAd = false
    private set

  private var loadTime: Long = 0
  private var lastShowTime: Long = 0
  private var defaultAdUnitId: String? = null
  private var isAutoShowEnabled: Boolean = false
  private var cooldownMs: Long = 20000L // 20s cooldown between auto-shows
  private var currentActivity: Activity? = null

  private val disabledActivityClasses = CopyOnWriteArraySet<Class<out Activity>>()

  /**
   * Initializes the AppOpenAdManager with automatic lifecycle observer.
   *
   * @param application The Application instance.
   * @param defaultAdUnitId The default AdMob Ad Unit ID for App Open Ads.
   * @param autoShowOnResume Whether to automatically show the ad when app enters foreground.
   * @param cooldownSeconds Minimum time in seconds between auto-show triggers (default 20s).
   */
  fun init(
    application: Application,
    defaultAdUnitId: String? = null,
    autoShowOnResume: Boolean = false,
    cooldownSeconds: Long = 20L,
  ) {
    this.defaultAdUnitId = defaultAdUnitId
    this.isAutoShowEnabled = autoShowOnResume
    this.cooldownMs = cooldownSeconds * 1000L

    application.registerActivityLifecycleCallbacks(this)
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
  }

  fun setAutoShowEnabled(enabled: Boolean) {
    isAutoShowEnabled = enabled
  }

  fun setCooldownSeconds(seconds: Long) {
    cooldownMs = seconds * 1000L
  }

  fun setDefaultAdUnitId(adUnitId: String) {
    defaultAdUnitId = adUnitId
  }

  fun disableForActivity(activityClass: Class<out Activity>) {
    disabledActivityClasses.add(activityClass)
  }

  fun enableForActivity(activityClass: Class<out Activity>) {
    disabledActivityClasses.remove(activityClass)
  }

  /**
   * Loads an App Open Ad.
   */
  fun loadAd(
    context: Context,
    adUnitId: String? = defaultAdUnitId,
    callback: AdEventListener? = null,
  ) {
    val unitId = adUnitId ?: defaultAdUnitId
    if (unitId.isNullOrEmpty()) {
      NextGenAds.logError("Cannot load App Open Ad: adUnitId is null or empty.")
      return
    }

    if (isLoadingAd || isAdAvailable()) {
      NextGenAds.log("App open ad is already loading or loaded.")
      return
    }

    try {
      isLoadingAd = true
      val adRequest = AdRequest.Builder(unitId).build()
      AppOpenAd.load(
        adRequest,
        object : AdLoadCallback<AppOpenAd> {
          override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
            isLoadingAd = false
            loadTime = Date().time
            NextGenAds.log("App open ad loaded successfully.")
            NextGenAds.runOnMainThread {
              callback?.onAdLoaded()
            }
          }

          override fun onAdFailedToLoad(adError: LoadAdError) {
            isLoadingAd = false
            NextGenAds.logError("App open ad failed to load: ${adError.message}")
            NextGenAds.runOnMainThread {
              callback?.onAdFailedToLoad(adError)
            }
          }
        },
      )
    } catch (e: Exception) {
      isLoadingAd = false
      NextGenAds.logError("Failed to request App Open Ad. Make sure NextGenAds.initialize(...) is called first: ${e.message}", e)
    }
  }

  /**
   * Loads and displays an App Open Ad specifically for SplashActivity with a timeout fallback.
   */
  fun showSplashAoa(
    activity: Activity,
    adUnitId: String? = defaultAdUnitId,
    timeoutMs: Long = 5000L,
    onComplete: () -> Unit,
  ) {
    val isCompleted = java.util.concurrent.atomic.AtomicBoolean(false)
    fun finishOnce() {
      if (!isCompleted.getAndSet(true)) {
        NextGenAds.runOnMainThread { onComplete() }
      }
    }

    // Timeout watchdog
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      finishOnce()
    }, timeoutMs)

    loadAd(
      context = activity,
      adUnitId = adUnitId,
      callback = object : AdEventListener {
        override fun onAdLoaded() {
          if (!isCompleted.get()) {
            showAdIfAvailable(
              activity = activity,
              onCompleteListener = { finishOnce() },
            )
          }
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
          finishOnce()
        }
      },
    )
  }

  /**
   * Checks if an ad was loaded within the last [numHours] hours (per policy, 4h max).
   */
  private fun wasLoadTimeLessThanNHoursAgo(numHours: Long = 4): Boolean {
    val dateDifference = Date().time - loadTime
    val numMilliSecondsPerHour = 3600000L
    return dateDifference < numMilliSecondsPerHour * numHours
  }

  /**
   * Checks if an ad is currently ready to show.
   */
  fun isAdAvailable(): Boolean {
    return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
  }

  /**
   * Shows the loaded App Open Ad if available.
   */
  fun showAdIfAvailable(
    activity: Activity,
    onCompleteListener: OnShowAdCompleteListener? = null,
    callback: AdEventListener? = null,
  ) {
    NextGenAds.runOnMainThread {
      if (isShowingAd) {
        NextGenAds.log("App open ad is already showing.")
        onCompleteListener?.onShowAdComplete()
        return@runOnMainThread
      }

      if (!isAdAvailable()) {
        NextGenAds.log("App open ad is not ready yet.")
        onCompleteListener?.onShowAdComplete()
        defaultAdUnitId?.let { loadAd(activity, it) }
        return@runOnMainThread
      }

      appOpenAd?.adEventCallback = object : AppOpenAdEventCallback {
        override fun onAdShowedFullScreenContent() {
          NextGenAds.log("App open ad showed.")
          NextGenAds.isFullScreenAdShowing = true
          lastShowTime = System.currentTimeMillis()
          NextGenAds.runOnMainThread { callback?.onAdShowed() }
        }

        override fun onAdDismissedFullScreenContent() {
          NextGenAds.log("App open ad dismissed.")
          appOpenAd = null
          isShowingAd = false
          NextGenAds.isFullScreenAdShowing = false
          lastShowTime = System.currentTimeMillis()
          NextGenAds.runOnMainThread {
            callback?.onAdDismissed()
            onCompleteListener?.onShowAdComplete()
          }
          defaultAdUnitId?.let { loadAd(activity, it) }
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
          NextGenAds.logError("App open ad failed to show: ${fullScreenContentError.message}")
          appOpenAd = null
          isShowingAd = false
          NextGenAds.isFullScreenAdShowing = false
          NextGenAds.runOnMainThread {
            callback?.onAdFailedToShow(fullScreenContentError)
            onCompleteListener?.onShowAdComplete()
          }
          defaultAdUnitId?.let { loadAd(activity, it) }
        }

        override fun onAdImpression() {
          NextGenAds.log("App open ad recorded impression.")
          NextGenAds.runOnMainThread { callback?.onAdImpression() }
        }

        override fun onAdClicked() {
          NextGenAds.log("App open ad recorded click.")
          NextGenAds.runOnMainThread { callback?.onAdClicked() }
        }
      }

      isShowingAd = true
      try {
        appOpenAd?.show(activity)
      } catch (e: Exception) {
        isShowingAd = false
        NextGenAds.isFullScreenAdShowing = false
        NextGenAds.logError("Error while showing App Open Ad: ${e.message}", e)
        onCompleteListener?.onShowAdComplete()
      }
    }
  }

  // Lifecycle Callbacks
  override fun onStart(owner: LifecycleOwner) {
    if (!isAutoShowEnabled || isShowingAd || NextGenAds.isFullScreenAdShowing) {
      NextGenAds.log("Skipping App Open Ad on resume: already showing full screen ad.")
      return
    }

    val now = System.currentTimeMillis()
    if (now - lastShowTime < cooldownMs) {
      NextGenAds.log("Skipping App Open Ad on resume: cooldown in effect.")
      return
    }

    val activity = currentActivity ?: return
    if (disabledActivityClasses.contains(activity.javaClass)) {
      NextGenAds.log("Skipping App Open Ad on disabled activity: ${activity.javaClass.simpleName}")
      return
    }

    showAdIfAvailable(activity)
  }

  override fun onActivityStarted(activity: Activity) {
    currentActivity = activity
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
  override fun onActivityResumed(activity: Activity) {}
  override fun onActivityPaused(activity: Activity) {}
  override fun onActivityStopped(activity: Activity) {}
  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
  override fun onActivityDestroyed(activity: Activity) {
    if (currentActivity == activity) {
      currentActivity = null
    }
  }
}
