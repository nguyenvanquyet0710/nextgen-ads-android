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

package com.nextgen.ads.callbacks

import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem

/**
 * Interface definition for a callback to be invoked when an ad lifecycle or interaction event occurs.
 */
interface AdEventListener {
  fun onAdLoaded() {}
  fun onAdFailedToLoad(error: LoadAdError) {}
  fun onAdShowed() {}
  fun onAdDismissed() {}
  fun onAdFailedToShow(error: FullScreenContentError) {}
  fun onAdImpression() {}
  fun onAdClicked() {}
  fun onAdPaid(value: AdValue) {}
  fun onAdRefreshed() {}
  fun onAdFailedToRefresh(error: LoadAdError) {}
}

/**
 * Functional interface for when a full screen ad is complete (dismissed or failed to show).
 */
fun interface OnShowAdCompleteListener {
  fun onShowAdComplete()
}

/**
 * Functional interface for when a user earns a reward from a rewarded ad.
 */
fun interface OnUserEarnedRewardListener {
  fun onUserEarnedReward(rewardItem: RewardItem)
}

/**
 * Interface for listening to Next-Gen SDK Preloader events.
 */
interface AdPreloadListener {
  fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {}
  fun onAdFailedToPreload(preloadId: String, error: LoadAdError) {}
  fun onAdsExhausted(preloadId: String) {}
}
