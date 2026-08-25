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

package com.nextgen.ads.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.nextgen.ads.NextGenAds

/**
 * Full-screen loading dialog shown before interstitial / rewarded / rewarded interstitial ads.
 * White full-bleed background with a centered spinner and message (e.g. "Loading...").
 */
class AdLoadingDialog(
  private val activity: Activity,
  private val message: String = "Loading...",
) {
  private var dialog: Dialog? = null

  fun show() {
    NextGenAds.runOnMainThread {
      if (activity.isFinishing || activity.isDestroyed) return@runOnMainThread

      try {
        if (dialog?.isShowing == true) return@runOnMainThread

        dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
          requestWindowFeature(Window.FEATURE_NO_TITLE)
          setCancelable(false)
          setCanceledOnTouchOutside(false)

          window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            setLayout(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // Draw under status/nav bars for a true full-screen look
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
              statusBarColor = Color.WHITE
              navigationBarColor = Color.WHITE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              decorView.systemUiVisibility =
                decorView.systemUiVisibility or
                  android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
          }

          val density = activity.resources.displayMetrics.density

          val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )

            val progressSize = (48 * density).toInt()
            val progressBar = ProgressBar(activity).apply {
              layoutParams = LinearLayout.LayoutParams(progressSize, progressSize)
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                indeterminateTintList = ColorStateList.valueOf(Color.BLACK)
              }
            }
            addView(progressBar)

            val textView = TextView(activity).apply {
              text = message
              setTextColor(Color.BLACK)
              setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
              gravity = Gravity.CENTER
              layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
              ).apply {
                topMargin = (16 * density).toInt()
              }
            }
            addView(textView)
          }

          setContentView(content)
          show()
        }
      } catch (e: Exception) {
        NextGenAds.logError("Error creating AdLoadingDialog: ${e.message}", e)
      }
    }
  }

  fun dismiss() {
    NextGenAds.runOnMainThread {
      try {
        if (dialog?.isShowing == true && !activity.isFinishing && !activity.isDestroyed) {
          dialog?.dismiss()
        }
      } catch (e: Exception) {
        NextGenAds.logError("Error dismissing AdLoadingDialog: ${e.message}", e)
      } finally {
        dialog = null
      }
    }
  }
}
