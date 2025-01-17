package com.github.teranes10.androidutils.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionUtil {
    private static final String TAG = "ConnectionUtil";

    private final ConnectionListener listener;
    private final String url;
    private final AtomicBoolean isAvailable = new AtomicBoolean(false);
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private HandlerThread handlerThread;
    private Handler handler;
    private int INTERNET_CHECKING_INTERVAL = 60 * 1000; // 1 min
    private int CONNECTION_TIMEOUT = 10 * 1000; // 10 sec

    public void setInternetCheckingInterval(int INTERNET_CHECKING_INTERVAL) {
        this.INTERNET_CHECKING_INTERVAL = INTERNET_CHECKING_INTERVAL;
    }

    public void setRequestConnectionTimeout(int CONNECTION_TIMEOUT) {
        this.CONNECTION_TIMEOUT = CONNECTION_TIMEOUT;
    }

    public ConnectionUtil(Context context, String url, ConnectionListener listener) {
        this.url = url;
        this.listener = listener;
        this.connectivityManager = context.getApplicationContext().getSystemService(ConnectivityManager.class);
        initializeNetworkCallback();
    }

    private void initializeNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                startInternetCheck();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                updateInternetAvailability(false);
                stopInternetCheck();
            }
        };

        connectivityManager.requestNetwork(networkRequest, networkCallback);
    }

    private void startInternetCheck() {
        if (handlerThread == null) {
            handlerThread = new HandlerThread(TAG);
            handlerThread.start();
        }
        if (handler == null) {
            handler = new Handler(handlerThread.getLooper());
        }

        handler.post(new InternetCheckRunnable());
    }

    private void stopInternetCheck() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (handlerThread != null) {
            handlerThread.quit();
            handlerThread = null;
        }
    }

    private class InternetCheckRunnable implements Runnable {
        private long delayFactor = 1;

        @Override
        public void run() {
            boolean isReachable = false;
            try {
                isReachable = NetworkUtil.isReachableAsync(url, CONNECTION_TIMEOUT).join();
                Log.i(TAG, "checking internet reachability: " + isReachable);
                updateInternetAvailability(isReachable);
            } catch (Exception e) {
                Log.e(TAG, "checking internet reachability: ", e);
            }

            if (handler != null) {
                if (!isReachable) {
                    // Limit delayFactor to a maximum of 4
                    if (++delayFactor > 4) {
                        delayFactor = 4;
                    }

                    // Calculate the next delay based on the delayFactor
                    long nextDelay = INTERNET_CHECKING_INTERVAL * delayFactor / 4;
                    Log.i(TAG, "Retrying in " + nextDelay + " ms");

                    // Post the retry task with the next delay
                    handler.postDelayed(this, nextDelay);
                } else {
                    // Reset delayFactor if internet is available
                    delayFactor = 1;
                    handler.postDelayed(this, INTERNET_CHECKING_INTERVAL);
                }
            }
        }
    }

    protected void updateInternetAvailability(boolean available) {
        if (isAvailable.getAndSet(available) != available) {
            listener.onInternetAvailabilityChanged(available);
            Log.i(TAG, available ? "Connected to the internet." : "Disconnected from the internet.");
        }
    }

    public void release() {
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        stopInternetCheck();
    }

    public interface ConnectionListener {
        void onInternetAvailabilityChanged(boolean isAvailable);
    }
}
