package com.github.teranes10.androidutils.ui;

import android.content.Context;
import android.view.WindowManager;

import com.github.teranes10.androidutils.databinding.CustomLoadingBinding;

public class FloatingLoading {
    private final FloatingAlert _floatingAlert;

    public FloatingLoading(Context context) {
        _floatingAlert = new FloatingAlert(context)
                .setWidth(WindowManager.LayoutParams.MATCH_PARENT)
                .setHeight(WindowManager.LayoutParams.MATCH_PARENT)
                .bindView((inflater, alert) -> {
                    CustomLoadingBinding binding =
                            CustomLoadingBinding.inflate(inflater, null, false);
                    return binding.getRoot();
                })
                .setAlpha(95)
                .build();
    }

    public void show() {
        if (_floatingAlert != null) {
            _floatingAlert.show();
        }
    }

    public void hide() {
        if (_floatingAlert != null) {
            _floatingAlert.close();
        }
    }
}
