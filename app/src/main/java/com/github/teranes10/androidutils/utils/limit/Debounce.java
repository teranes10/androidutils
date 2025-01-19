package com.github.teranes10.androidutils.utils.limit;

import android.os.Handler;

public class Debounce {
    private final Handler handler = new Handler();
    private Runnable runnable;

    public void debounce(Runnable action, long delayMillis) {
        if (runnable != null) {
            handler.removeCallbacksAndMessages(runnable);
        }

        runnable = action;
        handler.postDelayed(runnable, delayMillis);
    }
}
