package com.github.teranes10.androidutils.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import com.github.teranes10.androidutils.R

class Loading(private val context: Activity, private val fullScreen: Boolean = false) {

    private val dialog: Dialog = Dialog(context, if (fullScreen) R.style.FullScreenAlert else 0).apply {
        setContentView(R.layout.custom_loading)
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private val textView: TextView = dialog.findViewById(R.id.loading_text)

    fun start(message: String? = "") {
        if (context.isFinishing || context.isDestroyed || dialog.isShowing) {
            return
        }

        if (!message.isNullOrBlank()) {
            textView.text = message
            textView.visibility = View.VISIBLE

            val screenWidth = context.resources.configuration.smallestScreenWidthDp
            textView.textSize = when {
                screenWidth > 720 -> 16.0f
                screenWidth > 600 -> 14.0f
                else -> 12.0f
            }
        } else {
            textView.visibility = View.GONE
        }

        dialog.show()
    }

    fun stop() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
}

