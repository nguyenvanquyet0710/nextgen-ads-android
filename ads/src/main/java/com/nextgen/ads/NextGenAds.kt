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
  val isCheckTestDevice: Boolean = false,
  val isCheckOrganic: Boolean = false,
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

  /** Install referrer URL from Google Play (null if not yet fetched). */
  var referrerUrl: String? = null
    private set

  /** Whether this device has been detected as a test device (via ad headline analysis). */
  var isTestDevice: Boolean = false
    private set

  /**
   * Whether the install is organic (from Google Play search, not from ads).
   * Returns null if referrer has not been fetched yet.
   */
  val isOrganic: Boolean?
    get() {
      val url = referrerUrl ?: return null
      val lowerRef = url.lowercase()
      return when {
        // Case 1: Organic rõ ràng
        lowerRef.contains("utm_medium=organic") -> true
        // Case 2: Organic mặc định Play Store (không có utm_medium)
        lowerRef.contains("utm_source=google-play") &&
          !lowerRef.contains("utm_medium=") -> true
        // Case 3: Organic = not set
        lowerRef.contains("utm_medium=(not%20set)") ||
          lowerRef.contains("utm_medium=(not set)") ||
          lowerRef.contains("utm_medium=not%20set") -> true
        // Case 4: Empty hoặc null = organic
        lowerRef.isBlank() || lowerRef == "null" -> true
        else -> false
      }
    }

  /**
   * Whether ads should be shown. Combines all checks:
   * - [AdConfig.isAdsEnabled] = false → no ads (premium user)
   * - [AdConfig.isCheckOrganic] = true AND user is organic → no ads
   * - isTestDevice = true → no ads (once detected, always blocks)
   *
   * All ad helpers use this property to decide whether to load/show ads.
   */
  val canShowAds: Boolean
    get() {
      if (!adConfig.isAdsEnabled) return false
      if (isTestDevice) return false
      if (adConfig.isCheckOrganic && isOrganic == true) return false
      return true
    }

  /**
   * Same as [canShowAds] but also checks network connectivity.
   * Use this in ad load functions that have access to [Context].
   */
  fun canShowAds(context: Context): Boolean {
    if (!canShowAds) return false
    if (!isNetworkAvailable(context)) {
      log("canShowAds: No network connection")
      return false
    }
    return true
  }

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
   * Simplified initializer matching common ad library pattern.
   * Initializes SDK, sets ad config, and fetches install referrer for organic detection.
   *
   * @param context Application context.
   * @param appId Google AdMob App ID.
   * @param isDebug true = use test ad unit IDs, false = use real ads.
   * @param isEnableAds true = show ads, false = hide all ads (for premium users).
   * @param isCheckTestDevice true = detect test device via ad headline analysis.
   * @param isCheckOrganic true = check organic install, if organic → canShowAds returns false.
   * @param onComplete Callback when SDK + referrer are ready.
   */
  fun initAdmob(
    context: Context,
    appId: String = SAMPLE_APP_ID,
    isDebug: Boolean = false,
    isEnableAds: Boolean = true,
    isCheckTestDevice: Boolean = false,
    isCheckOrganic: Boolean = false,
    onComplete: (() -> Unit)? = null,
  ) {
    // 1. Set ad config
    adConfig = AdConfig(
      isTestMode = isDebug,
      isAdsEnabled = isEnableAds,
      isDebug = isDebug,
      isCheckTestDevice = isCheckTestDevice,
      isCheckOrganic = isCheckOrganic,
    )

    // 2. Initialize SDK on background thread
    CoroutineScope(Dispatchers.IO).launch {
      val appContext = context.applicationContext
      val initConfig = InitializationConfig.Builder(appId).build()

      MobileAds.initialize(appContext, initConfig) { initStatus ->
        cachedInitStatus = initStatus
        isInitializedFlag.set(true)
        isInitializingFlag.set(false)
        log("Mobile Ads SDK initialization complete: $initStatus")

        // 3. Fetch install referrer on main thread
        runOnMainThread {
          fetchInstallReferrer(appContext) {
            log("Install referrer fetched: $referrerUrl (organic=$isOrganic)")
            onComplete?.invoke()
          }
        }
      }
    }
  }

  /**
   * Fetches the install referrer from Google Play to determine if the install is organic.
   */
  private fun fetchInstallReferrer(context: Context, onComplete: () -> Unit) {
    try {
      val referrerClient = com.android.installreferrer.api.InstallReferrerClient.newBuilder(context).build()
      referrerClient.startConnection(object : com.android.installreferrer.api.InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseCode: Int) {
          try {
            if (responseCode == com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK) {
              val response = referrerClient.installReferrer
              referrerUrl = response.installReferrer
              log("Install referrer URL: $referrerUrl")
            } else {
              log("Install referrer failed with code: $responseCode")
            }
          } catch (e: Exception) {
            logError("Error fetching install referrer: ${e.message}", e)
          } finally {
            try { referrerClient.endConnection() } catch (_: Exception) {}
            onComplete()
          }
        }

        override fun onInstallReferrerServiceDisconnected() {
          log("Install referrer service disconnected")
          onComplete()
        }
      })
    } catch (e: Exception) {
      logError("Failed to start install referrer client: ${e.message}", e)
      onComplete()
    }
  }

  /**
   * Checks if the current device is a test device by analyzing the ad headline.
   * Call this after a native ad loads to detect test devices.
   *
   * @param headline The headline text from a loaded native ad (nativeAd.headline).
   */
  fun checkAdsTest(headline: String?) {
    if (!adConfig.isCheckTestDevice) {
      isTestDevice = false
      return
    }

    try {
      val testHeadline = headline.orEmpty().replace(" ", "").split(":").firstOrNull() ?: ""
      val testAdResponses = arrayOf(
        "TestAd",
        "Anunciodeprueba",
        "Annuncioditesto",
        "Testanzeige",
        "TesIklan",
        "Anúnciodeteste",
        "Тестовоеобъявление",
        "পরীক্ষামূলকবিজ্ঞাপন",
        "जाँचविज्ञापन",
        "إعلانتجريبي",
        "Quảngcáothửnghiệm",
        "テスト広告",
        "测试广告",
        "測試廣告",
        "테스트광고",
        "Testreklam",
        "โฆษณาทดสอบ",
      )
      isTestDevice = testAdResponses.contains(testHeadline)
      log("checkAdsTest: headline='$testHeadline', isTestDevice=$isTestDevice")
    } catch (e: Exception) {
      isTestDevice = true
      logError("checkAdsTest error: ${e.message}", e)
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
