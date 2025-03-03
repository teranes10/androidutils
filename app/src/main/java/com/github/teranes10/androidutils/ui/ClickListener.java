package com.github.teranes10.androidutils.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.media.ToneGenerator;
import android.view.View;

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

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.9f, 1f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(v, "translationY", 0f, 10f, 0f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, translateY);
        animatorSet.setDuration(150);
        animatorSet.start();

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
