package com.github.teranes10.androidutils.utils;

import android.content.Context;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class DefaultConnectionUtil extends EventUtil<ConnectionUtil.ConnectionListener> {
    private final ConnectionUtil _connectionUtil;

    private static DefaultConnectionUtil instance;
    private static String _url = "https://www.google.com/generate_204";
    private static int _interval = 30 * 1000;

    private DefaultConnectionUtil(Context ctx, String url, int interval) {
        _connectionUtil = new ConnectionUtil(ctx, url,
                isAvailable ->
                        notifyListeners(x -> x.onInternetAvailabilityChanged(isAvailable)));

        _connectionUtil.setInternetCheckingInterval(interval);
    }

    private static synchronized DefaultConnectionUtil getInstance(Context ctx) {
        if (instance == null) {
            instance = new DefaultConnectionUtil(ctx, _url, _interval);
        }

        return instance;
    }

    public static void setUrl(String _url) {
        DefaultConnectionUtil._url = _url;
    }

    public static void setInterval(int _interval) {
        DefaultConnectionUtil._interval = _interval;
    }

    public static void updateInternetAvailability(boolean isConnected) {
        if (instance != null) {
            instance._connectionUtil.updateInternetAvailability(isConnected);
        }
    }

    public static void setListener(Context context, ConnectionUtil.ConnectionListener listener) {
        DefaultConnectionUtil instance = getInstance(context);
        instance.addListener(listener);
    }

    public static void removeListener(Context context, ConnectionUtil.ConnectionListener listener) {
        if (instance == null) {
            return;
        }

        if (instance.removeListener(listener) && instance.getListenerCount() == 0) {
            instance._connectionUtil.release();
            instance = null;
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
