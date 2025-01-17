package com.github.teranes10.androidutils.ui;

import android.media.ToneGenerator;
import android.view.View;
import android.view.animation.TranslateAnimation;

import com.github.teranes10.androidutils.helpers.Tone;

public class ClickListener {
    public static long lastClickTime = 0;
    public static final long DOUBLE_CLICK_TIME_DELTA = 750;

    public static boolean isDoubleClick() {
        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            return true;
        }

        lastClickTime = clickTime;
        return false;
    }

    public static boolean isDoubleClick(View v) {
        TranslateAnimation animation = new TranslateAnimation(-50.0f, 0.0f, 0.0f, 0.0f);
        animation.setDuration(50);
        animation.setFillAfter(false);
        animation.setRepeatCount(2);
        animation.setRepeatMode(2);
        v.startAnimation(animation);

        Tone.play(ToneGenerator.TONE_PROP_BEEP, 10);
        return isDoubleClick();
    }

    public static void setOnClickListener(View view, View.OnClickListener listener) {
        view.setOnClickListener(v -> {
            if (isDoubleClick(v)) {
                return;
            }

            listener.onClick(v);
        });
    }
}
