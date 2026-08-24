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

package com.nextgen.ads

import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ad format types for resolving test/live ad unit IDs.
 */
enum class AdFormat {
  BANNER, INTERSTITIAL, REWARDED, REWARDED_INTERSTITIAL, NATIVE, APP_OPEN
}

/**
 * Google's official sample test Ad Unit IDs.
 */
object TestAdUnitIds {
  const val BANNER = "ca-app-pub-3940256099942544/9214589741"
  const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
  const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
  const val REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379"
  const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
  const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
}

/**
 * Global ad configuration.
 *
 * @param isTestMode When true, all ad unit IDs are automatically replaced with Google's sample test IDs.
 * @param isAdsEnabled When false, all ad loading/showing is suppressed (useful for premium users).
 * @param isDebug When true, enables verbose logging via NextGenAds.log().
 */
data class AdConfig(
  val isTestMode: Boolean = false,
  val isAdsEnabled: Boolean = true,
  val isDebug: Boolean = false,
)

/**
 * Main entry point for initializing and configuring Google Mobile Ads Next-Gen SDK.
 */
object NextGenAds {
  const val TAG = "NextGenAds"

  // Google sample AdMob App ID
  const val SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713"

  /** Global ad configuration. Set this in Application.onCreate() before any ad calls. */
  var adConfig: AdConfig = AdConfig()
    set(value) {
      field = value
      isDebug = value.isDebug
    }

  private val isInitializedFlag = AtomicBoolean(false)
  private val isInitializingFlag = AtomicBoolean(false)
  private var cachedInitStatus: InitializationStatus? = null
  private val initCallbacks = mutableListOf<(InitializationStatus) -> Unit>()

  var isDebug: Boolean = false

  var isFullScreenAdShowing: Boolean = false
    internal set(value) {
      if (field != value) {
        field = value
        runOnMainThread { onAdVisibilityChanged?.invoke(value) }
      }
    }

  /**
   * Global listener invoked whenever ANY full-screen ad (AppOpen, Interstitial, Rewarded) opens or closes.
   * Perfect for pausing/resuming game background music and sound effects in 1 single place.
   */
  var onAdVisibilityChanged: ((isShowing: Boolean) -> Unit)? = null

  val isInitialized: Boolean
    get() = isInitializedFlag.get()

  /**
   * Resolves the ad unit ID based on current [adConfig].
   * Returns the Google sample test ID if [AdConfig.isTestMode] is true.
   */
  fun resolveAdUnitId(realAdUnitId: String, format: AdFormat): String {
    if (!adConfig.isTestMode) return realAdUnitId
    return when (format) {
      AdFormat.BANNER -> TestAdUnitIds.BANNER
      AdFormat.INTERSTITIAL -> TestAdUnitIds.INTERSTITIAL
      AdFormat.REWARDED -> TestAdUnitIds.REWARDED
      AdFormat.REWARDED_INTERSTITIAL -> TestAdUnitIds.REWARDED_INTERSTITIAL
      AdFormat.NATIVE -> TestAdUnitIds.NATIVE
      AdFormat.APP_OPEN -> TestAdUnitIds.APP_OPEN
    }
  }

  /**
   * Checks whether the given ad unit ID is a Google sample test ID.
   */
  fun isTestAdUnitId(adUnitId: String): Boolean {
    return adUnitId.startsWith("ca-app-pub-3940256099942544")
  }

  /**
   * Initializes the Google Mobile Ads Next-Gen SDK.
   *
   * @param context Application or Activity context.
   * @param appId Google AdMob App ID (e.g. ca-app-pub-3940256099942544~3347511713).
   * @param testDeviceIds List of hashed test device IDs for test ads.
   * @param ageRestrictedTreatment Optional AgeRestrictedTreatment (CHILD, TEEN, UNSPECIFIED).
   * @param onComplete Callback invoked when initialization finishes.
   */
  fun initialize(
    context: Context,
    appId: String = SAMPLE_APP_ID,
    testDeviceIds: List<String> = emptyList(),
    ageRestrictedTreatment: AgeRestrictedTreatment? = null,
    onComplete: ((InitializationStatus) -> Unit)? = null,
  ) {
    if (isInitializedFlag.get()) {
      log("NextGenAds is already initialized.")
      cachedInitStatus?.let { status ->
        runOnMainThread { onComplete?.invoke(status) }
      }
      return
    }

    onComplete?.let {
      synchronized(initCallbacks) {
        initCallbacks.add(it)
      }
    }

    if (isInitializingFlag.getAndSet(true)) {
      log("NextGenAds is currently initializing. Callback queued.")
      return
    }

    val appContext = context.applicationContext
    log("Initializing Google Mobile Ads Next-Gen SDK with App ID: $appId")

    val initConfig = InitializationConfig.Builder(appId).build()

    MobileAds.initialize(appContext, initConfig) { initStatus ->
      cachedInitStatus = initStatus
      isInitializedFlag.set(true)
      isInitializingFlag.set(false)
      log("Mobile Ads SDK initialization complete: $initStatus")

      val callbacks = synchronized(initCallbacks) {
        val list = initCallbacks.toList()
        initCallbacks.clear()
        list
      }
      runOnMainThread {
        callbacks.forEach { it.invoke(initStatus) }
      }
    }

    if (testDeviceIds.isNotEmpty() || ageRestrictedTreatment != null) {
      setRequestConfiguration(
        testDeviceIds = testDeviceIds,
        ageRestrictedTreatment = ageRestrictedTreatment,
      )
    }
  }

  /**
   * Configures global ad request settings such as test device IDs and age-restricted treatment.
   */
  fun setRequestConfiguration(
    testDeviceIds: List<String> = emptyList(),
    ageRestrictedTreatment: AgeRestrictedTreatment? = null,
    maxAdContentRating: RequestConfiguration.MaxAdContentRating? = null,
  ) {
    val builder = RequestConfiguration.Builder()

    if (testDeviceIds.isNotEmpty()) {
      builder.setTestDeviceIds(testDeviceIds)
    }
    ageRestrictedTreatment?.let {
      builder.setAgeRestrictedTreatment(it)
    }
    maxAdContentRating?.let {
      builder.setMaxAdContentRating(it)
    }

    MobileAds.setRequestConfiguration(builder.build())
    log("Updated RequestConfiguration: testDevices=${testDeviceIds.size}, ageTreatment=$ageRestrictedTreatment")
  }

  /**
   * Checks if the device has an active internet connection.
   */
  fun isNetworkAvailable(context: Context): Boolean =
    com.nextgen.ads.utils.NetworkHelper.isNetworkAvailable(context)

  /**
   * Observes real-time internet connectivity status as a Kotlin Flow.
   */
  fun observeNetworkState(context: Context) =
    com.nextgen.ads.utils.NetworkHelper.observeNetworkState(context)

  internal fun log(message: String) {
    if (isDebug) {
      Log.d(TAG, message)
    }
  }

  internal fun logError(message: String, throwable: Throwable? = null) {
    Log.e(TAG, message, throwable)
  }

  internal fun runOnMainThread(action: () -> Unit) {
    if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
      action()
    } else {
      android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }
  }
}
