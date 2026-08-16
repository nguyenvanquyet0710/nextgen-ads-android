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
  private var defaultAdUnitId: String? = null
  private var isAutoShowEnabled: Boolean = false
  private var currentActivity: Activity? = null

  private val disabledActivityClasses = CopyOnWriteArraySet<Class<out Activity>>()

  /**
   * Initializes the AppOpenAdManager with automatic lifecycle observer.
   *
   * @param application The Application instance.
   * @param defaultAdUnitId The default AdMob Ad Unit ID for App Open Ads.
   * @param autoShowOnResume Whether to automatically show the ad when app enters foreground.
   */
  fun init(
    application: Application,
    defaultAdUnitId: String? = null,
    autoShowOnResume: Boolean = false,
  ) {
    this.defaultAdUnitId = defaultAdUnitId
    this.isAutoShowEnabled = autoShowOnResume

    application.registerActivityLifecycleCallbacks(this)
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
  }

  fun setAutoShowEnabled(enabled: Boolean) {
    isAutoShowEnabled = enabled
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
          callback?.onAdLoaded()
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
          isLoadingAd = false
          NextGenAds.logError("App open ad failed to load: ${adError.message}")
          callback?.onAdFailedToLoad(adError)
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
    if (isShowingAd) {
      NextGenAds.log("App open ad is already showing.")
      onCompleteListener?.onShowAdComplete()
      return
    }

    if (!isAdAvailable()) {
      NextGenAds.log("App open ad is not ready yet.")
      onCompleteListener?.onShowAdComplete()
      defaultAdUnitId?.let { loadAd(activity, it) }
      return
    }

    appOpenAd?.adEventCallback = object : AppOpenAdEventCallback {
      override fun onAdShowedFullScreenContent() {
        NextGenAds.log("App open ad showed.")
        callback?.onAdShowed()
      }

      override fun onAdDismissedFullScreenContent() {
        NextGenAds.log("App open ad dismissed.")
        appOpenAd = null
        isShowingAd = false
        callback?.onAdDismissed()
        onCompleteListener?.onShowAdComplete()
        defaultAdUnitId?.let { loadAd(activity, it) }
      }

      override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
        NextGenAds.logError("App open ad failed to show: ${fullScreenContentError.message}")
        appOpenAd = null
        isShowingAd = false
        callback?.onAdFailedToShow(fullScreenContentError)
        onCompleteListener?.onShowAdComplete()
        defaultAdUnitId?.let { loadAd(activity, it) }
      }

      override fun onAdImpression() {
        NextGenAds.log("App open ad recorded impression.")
        callback?.onAdImpression()
      }

      override fun onAdClicked() {
        NextGenAds.log("App open ad recorded click.")
        callback?.onAdClicked()
      }
    }

    isShowingAd = true
    appOpenAd?.show(activity)
  }

  // Lifecycle Callbacks
  override fun onStart(owner: LifecycleOwner) {
    if (!isAutoShowEnabled) return

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
