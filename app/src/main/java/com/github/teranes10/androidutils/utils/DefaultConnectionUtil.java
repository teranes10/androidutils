package com.github.teranes10.androidutils.utils;

import android.content.Context;

import java.lang.ref.WeakReference;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class DefaultConnectionUtil extends EventUtil<ConnectionUtil.ConnectionListener> {
    private static volatile WeakReference<DefaultConnectionUtil> instanceWeakRef;
    private static String _url = "https://www.google.com/generate_204";
    private static int _interval = 30 * 1000;

    private final ConnectionUtil _connectionUtil;

    private DefaultConnectionUtil(Context ctx, String url, int interval) {
        _connectionUtil = new ConnectionUtil(ctx, url,
                isAvailable ->
                        notifyListeners(x -> x.onInternetAvailabilityChanged(isAvailable)));

        _connectionUtil.setInternetCheckingInterval(interval);
    }

    @Override
    public boolean addListener(ConnectionUtil.ConnectionListener listener) {
        return super.addListener(listener);
    }

    @Override
    public synchronized boolean removeListener(ConnectionUtil.ConnectionListener listener) {
        if (super.removeListener(listener)) {
            if (getListenerCount() == 0) {
                _connectionUtil.release();

                if (instanceWeakRef != null) {
                    instanceWeakRef.clear();
                    instanceWeakRef = null;
                }
            }

            return true;
        }

        return false;
    }

    public static synchronized DefaultConnectionUtil getInstance(Context ctx) {
        DefaultConnectionUtil instance = instanceWeakRef != null ? instanceWeakRef.get() : null;
        if (instance == null) {
            instance = new DefaultConnectionUtil(ctx, _url, _interval);
            instanceWeakRef = new WeakReference<>(instance);
        }

        return instance;
    }

    protected static void setup(String url, int interval) {
        _url = url;
        _interval = interval;
    }

    private static synchronized void updateInternetAvailability(boolean isConnected) {
        DefaultConnectionUtil instance = instanceWeakRef != null ? instanceWeakRef.get() : null;
        if (instance != null) {
            instance._connectionUtil.updateInternetAvailability(isConnected);
        }
    }

    public static Interceptor interceptor() {
        return chain -> {
            Request request = chain.request();
            try {
                Response response = chain.proceed(request);
                updateInternetAvailability(true);
                return response;
            } catch (Exception e) {
                updateInternetAvailability(false);
                throw e;
            }
        };
    }
}
