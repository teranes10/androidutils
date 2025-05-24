package com.github.teranes10.androidutils.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.github.teranes10.androidutils.databinding.CustomLoadingBinding

class FloatingLoading(context: Context, text: String? = null) {
    private val _floatingAlert: FloatingAlert
    private var _binding: CustomLoadingBinding? = null

    init {
        _floatingAlert = FloatingAlert(context)
            .setWidth(WindowManager.LayoutParams.MATCH_PARENT)
            .setHeight(WindowManager.LayoutParams.MATCH_PARENT)
            .bindView { inflater: LayoutInflater?, alert: FloatingAlert ->
                _binding = CustomLoadingBinding.inflate(inflater!!, null, false)

                if (text != null) {
                    _binding?.loadingText?.text = text
                    _binding?.loadingText?.visibility = View.VISIBLE
                }

                _binding?.root
            }
            .setAlpha(95f)
            .build()
    }

    fun show() {
        _floatingAlert.show()
    }

    fun hide() {
        _floatingAlert.close()
    }

    fun setText(text: String) {
        if (_floatingAlert.isShowing && _binding != null) {
            _binding?.loadingText?.text = text
        }
    }
}
