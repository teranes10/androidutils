package com.github.teranes10.androidutils.utils;

import android.os.Handler;
import android.os.HandlerThread;

public abstract class TimerUtil {
    private HandlerThread _handlerThread;
    private Handler _handler;
    private Runnable _runnable;

    public abstract String getTag();

    public abstract long getInterval();

    public abstract void execute();

    public void start() {
        if (_handlerThread == null) {
            _handlerThread = new HandlerThread(getTag());
        }
        if (!_handlerThread.isAlive()) {
            _handlerThread.start();
        }
        if (_handler == null) {
            _handler = new Handler(_handlerThread.getLooper());
        }

        _runnable = new Runnable() {
            @Override
            public void run() {
                execute();

                long interval = getInterval();
                if (interval > 0) {
                    _handler.postDelayed(this, interval);
                }
            }
        };

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
}
