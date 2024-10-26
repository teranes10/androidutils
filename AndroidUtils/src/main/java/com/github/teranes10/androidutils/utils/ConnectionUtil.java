package com.github.teranes10.androidutils.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ConnectionUtil {
    private static final String TAG = "ConnectionUtil";
    private static final int INTERNET_CHECKING_INTERVAL = 60 * 1000; //1min
    private static final int CONNECTION_TIMEOUT = 10 * 1000; //10sec
    private ConnectivityManager _connectivityManager;
    private ConnectivityManager.NetworkCallback _networkCallback;
    private Handler _handler;
    private static ConnectionListener _listener;
    private final String _url;
    private final Context _ctx;
    private static final AtomicBoolean _isAvailable = new AtomicBoolean(false);

    public ConnectionUtil(Context ctx, String url, ConnectionListener listener) {
        _ctx = ctx;
        _url = url;
        _listener = listener;
    }

    private static void update(boolean isAvailable) {
        if (_listener != null) {
            if (_isAvailable.getAndSet(isAvailable) != isAvailable) {
                _listener.onInternetAvailabilityChanged(isAvailable);
            }
        }
    }

    public void startUpdates() {
        try {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .build();

            _networkCallback = new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    if (_handler != null) {
                        return;
                    }

                    Log.i(TAG, "onAvailable: ");
                    _handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            NetworkUtil.isReachableAsync(_ctx, _url, CONNECTION_TIMEOUT).thenAcceptAsync(isReachable -> {
                                Log.i(TAG, (isReachable ? "" : "not ") + "connected to the internet.");
                                update(isReachable);
                                _handler.postDelayed(this, INTERNET_CHECKING_INTERVAL);
                            });
                        }
                    };

                    _handler.post(runnable);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    Log.i(TAG, "onLost: ");
                    update(false);
                    if (_handler != null) {
                        _handler.removeCallbacksAndMessages(null);
                        _handler = null;
                    }
                }
            };

            _connectivityManager = _ctx.getSystemService(ConnectivityManager.class);
            _connectivityManager.requestNetwork(networkRequest, _networkCallback);
        } catch (Exception e) {
            Log.e(TAG, "startConnectionStatusUpdates: ", e);
        }
    }

    public void stopUpdates() {
        if (_connectivityManager != null && _networkCallback != null) {
            _connectivityManager.unregisterNetworkCallback(_networkCallback);
        }
        if (_handler != null) {
            _handler.removeCallbacksAndMessages(null);
            _handler = null;
        }
    }

    public static Interceptor interceptor() {
        return chain -> {
            Request request = chain.request();
            try {
                Response response = chain.proceed(request);
                update(true);
                return response;
            } catch (Exception e) {
                update(false);
                throw e;
            }
        };
    }

    public interface ConnectionListener {
        void onInternetAvailabilityChanged(boolean isAvailable);
    }
}
