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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.nextgen.ads.NextGenAds

/**
 * Lightweight loading dialog shown before presenting full-screen ads.
 */
class AdLoadingDialog(private val activity: Activity, private val message: String = "Loading...") {
  private var dialog: Dialog? = null

  fun show() {
    NextGenAds.runOnMainThread {
      if (activity.isFinishing || activity.isDestroyed) return@runOnMainThread

      try {
        dialog = Dialog(activity).apply {
          requestWindowFeature(Window.FEATURE_NO_TITLE)
          setCancelable(false)
          setCanceledOnTouchOutside(false)
          window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

          val density = activity.resources.displayMetrics.density

          val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (24 * density).toInt()
            val padV = (16 * density).toInt()
            setPadding(padH, padV, padH, padV)

            val shape = GradientDrawable().apply {
              setColor(Color.parseColor("#E6212121"))
              cornerRadius = 12 * density
            }
            background = shape

            val progressBar = ProgressBar(activity).apply {
              val size = (32 * density).toInt()
              layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (16 * density).toInt()
              }
            }
            addView(progressBar)

            val textView = TextView(activity).apply {
              text = message
              setTextColor(Color.WHITE)
              textSize = 15f
              layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
              )
            }
            addView(textView)
          }

          setContentView(rootLayout)
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
