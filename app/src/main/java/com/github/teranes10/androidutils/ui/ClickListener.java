package com.github.teranes10.androidutils.ui;

import android.media.ToneGenerator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.github.teranes10.androidutils.helpers.Tone;

public class ClickListener {
    public static long lastClickTime = 0;
    public static final long DOUBLE_CLICK_TIME_DELTA = 500;

    public static boolean isDoubleClick() {
        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            return true;
        }

        lastClickTime = clickTime;
        return false;
    }

    public static boolean isDoubleClick(View v) {
        if (isDoubleClick()) {
            return true;
        }

        TranslateAnimation animation = new TranslateAnimation(-50.0f, 0.0f, 0.0f, 0.0f);
        animation.setDuration(50);
        animation.setFillAfter(false);
        animation.setRepeatCount(2);
        animation.setRepeatMode(Animation.REVERSE);
        v.startAnimation(animation);

        Tone.play(ToneGenerator.TONE_PROP_BEEP, 10);
        return false;
    }

    public static void setOnClickListener(View view, View.OnClickListener listener) {
        view.setOnClickListener(v -> {
            if (isDoubleClick(v)) {
                return;
            }

            listener.onClick(v);
        });
    }

    public static void setOnLongClickListener(View view, View.OnClickListener listener) {
        view.setOnLongClickListener(v -> {
            if (isDoubleClick(v)) {
                return false;
            }

            listener.onClick(v);
            return true;
        });
    }
}
