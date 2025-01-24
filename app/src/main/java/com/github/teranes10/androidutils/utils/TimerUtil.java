package com.github.teranes10.androidutils.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.concurrent.CompletableFuture;

public abstract class TimerUtil {
    private HandlerThread _handlerThread;
    private Handler _handler;
    private Runnable _runnable;

    public abstract String getTag();

    public abstract long getInterval();

    public abstract void execute();

    public void start() {
        if (_handlerThread != null && _handlerThread.isAlive()) {
            return; // Already started
        }

        if (_handlerThread == null) {
            _handlerThread = new HandlerThread(getTag());
            _handlerThread.start();
        }

        if (_handler == null) {
            _handler = new Handler(_handlerThread.getLooper());
        }

        _runnable = () -> CompletableFuture.runAsync(() -> {
            try {
                execute();
            } catch (Exception e) {
                Log.e(getTag(), "execute: ", e);
            }
        }).thenRun(() -> {
            long interval = getInterval();
            if (interval > 0) {
                _handler.postDelayed(_runnable, interval);
            }
        });

        _handler.post(_runnable);
    }

    public void stop() {
        if (_handler != null) {
            _handler.removeCallbacksAndMessages(null);
            _handler = null;
        }
        if (_handlerThread != null) {
            _handlerThread.quit();
            _handlerThread = null;
        }
    }

    public void reset() {
        if (_handler != null && _runnable != null) {
            _handler.removeCallbacksAndMessages(null);
            _handler.postDelayed(_runnable, getInterval());
        }
    }

    public void run() {
        if (_handler != null && _runnable != null) {
            _handler.removeCallbacksAndMessages(null);
            _handler.post(_runnable);
        }
    }
}
