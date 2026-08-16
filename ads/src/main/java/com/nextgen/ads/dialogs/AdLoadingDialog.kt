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
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
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
 * Premium, modern loading dialog shown smoothly before presenting full-screen ads.
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

        dialog = Dialog(activity).apply {
          requestWindowFeature(Window.FEATURE_NO_TITLE)
          setCancelable(false)
          setCanceledOnTouchOutside(false)

          window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.35f)
          }

          val isNightMode = (activity.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

          val density = activity.resources.displayMetrics.density

          // Container Card
          val cardLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val padH = (22 * density).toInt()
            val padV = (16 * density).toInt()
            setPadding(padH, padV, padH, padV)

            val bgCardColor = if (isNightMode) Color.parseColor("#282828") else Color.parseColor("#FFFFFF")
            val cardShape = GradientDrawable().apply {
              setColor(bgCardColor)
              cornerRadius = 16 * density
              if (!isNightMode) {
                setStroke((1 * density).toInt(), Color.parseColor("#E0E0E0"))
              }
            }
            background = cardShape
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
              elevation = 12 * density
            }

            // Material Google Blue ProgressBar
            val progressBar = ProgressBar(activity).apply {
              val size = (28 * density).toInt()
              layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (14 * density).toInt()
              }
              val accentColor = Color.parseColor("#1A73E8") // Google Material Blue
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                indeterminateTintList = ColorStateList.valueOf(accentColor)
              }
            }
            addView(progressBar)

            // Crisp Typography
            val textColor = if (isNightMode) Color.parseColor("#F1F3F4") else Color.parseColor("#202124")
            val textView = TextView(activity).apply {
              text = message
              setTextColor(textColor)
              setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
              letterSpacing = 0.01f
              layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
              )
            }
            addView(textView)
          }

          setContentView(cardLayout)
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
