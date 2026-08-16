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

package com.nextgen.ads.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.nextgen.ads.NextGenAds

/**
 * Helper class for managing GDPR / CCPA user consent using Google's User Messaging Platform (UMP).
 */
class ConsentManager private constructor(context: Context) {
  private val consentInformation: ConsentInformation =
    UserMessagingPlatform.getConsentInformation(context.applicationContext)

  /**
   * Helper property to determine if the app can request ads based on user consent.
   */
  val canRequestAds: Boolean
    get() = consentInformation.canRequestAds()

  /**
   * Helper property to determine if the privacy options form is required.
   */
  val isPrivacyOptionsRequired: Boolean
    get() = consentInformation.privacyOptionsRequirementStatus ==
      ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

  /**
   * Requests consent information update and automatically loads/shows the consent form if required.
   *
   * @param activity The current activity.
   * @param testDeviceId Optional test device hashed ID for debugging consent.
   * @param isDebugGeographyEEA If true, simulates European Economic Area (EEA) location.
   * @param onConsentGatheringComplete Callback invoked when consent gathering is finished.
   */
  fun gatherConsent(
    activity: Activity,
    testDeviceId: String? = null,
    isDebugGeographyEEA: Boolean = false,
    timeoutMs: Long = 3500L,
    onConsentGatheringComplete: (FormError?) -> Unit,
  ) {
    val isCompleted = java.util.concurrent.atomic.AtomicBoolean(false)
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val timeoutRunnable = Runnable {
      if (!isCompleted.getAndSet(true)) {
        NextGenAds.log("Consent gathering timeout ($timeoutMs ms). Proceeding.")
        NextGenAds.runOnMainThread { onConsentGatheringComplete(null) }
      }
    }
    handler.postDelayed(timeoutRunnable, timeoutMs)

    fun finishConsent(error: FormError?) {
      handler.removeCallbacks(timeoutRunnable)
      if (!isCompleted.getAndSet(true)) {
        NextGenAds.runOnMainThread { onConsentGatheringComplete(error) }
      }
    }

    try {
      val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
      if (!testDeviceId.isNullOrEmpty()) {
        debugSettingsBuilder.addTestDeviceHashedId(testDeviceId)
      }
      if (isDebugGeographyEEA) {
        debugSettingsBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
      }

      val params = ConsentRequestParameters.Builder()
        .setConsentDebugSettings(debugSettingsBuilder.build())
        .build()

      consentInformation.requestConsentInfoUpdate(
        activity,
        params,
        {
          UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            NextGenAds.log("Consent form dismissed or completed. FormError: $formError")
            finishConsent(formError)
          }
        },
        { requestConsentError ->
          NextGenAds.logError("Error requesting consent info update: ${requestConsentError.message}")
          finishConsent(requestConsentError)
        },
      )
    } catch (e: Exception) {
      NextGenAds.logError("Error in gatherConsent: ${e.message}", e)
      finishConsent(null)
    }
  }

  /**
   * Shows the privacy options form so users can update their consent choices anytime.
   */
  fun showPrivacyOptionsForm(
    activity: Activity,
    onDismissListener: ConsentForm.OnConsentFormDismissedListener,
  ) {
    UserMessagingPlatform.showPrivacyOptionsForm(activity, onDismissListener)
  }

  /**
   * Resets consent state for testing purposes.
   */
  fun resetConsent() {
    consentInformation.reset()
  }

  companion object {
    @Volatile
    private var instance: ConsentManager? = null

    fun getInstance(context: Context): ConsentManager =
      instance ?: synchronized(this) {
        instance ?: ConsentManager(context).also { instance = it }
      }
  }
}
