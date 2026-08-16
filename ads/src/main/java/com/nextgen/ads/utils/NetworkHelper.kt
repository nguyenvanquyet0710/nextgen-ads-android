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

package com.nextgen.ads.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Utility helper for checking and observing network connectivity.
 */
object NetworkHelper {

  /**
   * Checks whether the device is currently connected to an active internet network.
   *
   * @param context Application or Activity context.
   * @return true if internet is available and validated, false otherwise.
   */
  fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }

  /**
   * Observes real-time network connectivity changes as a Kotlin [Flow].
   * Emits `true` when connected and `false` when disconnected.
   *
   * @param context Application or Activity context.
   */
  fun observeNetworkState(context: Context): Flow<Boolean> = callbackFlow {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    if (connectivityManager == null) {
      trySend(false)
      close()
      return@callbackFlow
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(true)
      }

      override fun onLost(network: Network) {
        trySend(isNetworkAvailable(context))
      }

      override fun onUnavailable() {
        trySend(false)
      }
    }

    // Emit initial status
    trySend(isNetworkAvailable(context))

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    connectivityManager.registerNetworkCallback(request, callback)

    awaitClose {
      connectivityManager.unregisterNetworkCallback(callback)
    }
  }.distinctUntilChanged()
}
