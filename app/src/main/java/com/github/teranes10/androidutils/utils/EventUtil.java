package com.github.teranes10.androidutils.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class EventUtil<T> {
    private final List<T> listeners = Collections.synchronizedList(new ArrayList<>());
    private Handler handler;

    protected boolean addListener(T listener) {
        if (listener == null) return false;

        synchronized (listeners) {
            if (listeners.contains(listener)) return false;
            return listeners.add(listener);
        }
    }

    protected boolean removeListener(T listener) {
        if (listener == null) return false;

        synchronized (listeners) {
            return listeners.remove(listener);
        }
    }

    protected void notifyListeners(Consumer<T> func) {
        List<T> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        for (T listener : listenersCopy) {
            func.accept(listener);
        }
    }

    protected int getListenerCount() {
        synchronized (listeners) {
            return listeners.size();
        }
    }

    protected void notifyListeners(Consumer<T> func, int timerInMillis) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> notifyListeners(func), timerInMillis);
    }
}
