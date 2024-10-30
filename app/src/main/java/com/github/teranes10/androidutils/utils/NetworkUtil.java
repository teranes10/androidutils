package com.github.teranes10.androidutils.utils;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NetworkUtil {
    private static final int NETWORK_UTIL_CONNECTION_TAG = 10000;
    private static final String TAG = "NetworkUtil";

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }

        return false;
    }

    public static String getSSID(Context context) {
        return getWifiInfo(context).getSSID();
    }

    public static String getBSSID(Context context) {
        return getWifiInfo(context).getBSSID();
    }

    public static String getLinkSpeed(Context context) {
        return getWifiInfo(context).getLinkSpeed() + "Mbps";
    }

    public static String getIpAddress(Context context) {
        WifiInfo wifiInfo = getWifiInfo(context);

        @SuppressLint("DefaultLocale") String ipAddress = String.format("%d.%d.%d.%d",
                (wifiInfo.getIpAddress() & 0xff), (wifiInfo.getIpAddress() >> 8 & 0xff),
                (wifiInfo.getIpAddress() >> 16 & 0xff), (wifiInfo.getIpAddress() >> 24 & 0xff));

        if (ipAddress.equals("0.0.0.0")) {
            try {
                List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface networkInterface : networkInterfaces) {
                    List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                    for (InetAddress address : inetAddresses) {
                        if (!address.isLoopbackAddress()) {
                            return address.getHostAddress();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return "";
        }

        return ipAddress;
    }

    public static String getSignalStrength(Context context) {
        WifiInfo wifiInfo = getWifiInfo(context);
        @SuppressLint("DefaultLocale") String RSSI = String.format("%d", wifiInfo.getRssi());
        return RSSI + "dB";
    }

    public static CellTowerInfo getCellInfo(Context ctx) {
        TelephonyManager tel = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            List<CellInfo> infoList = tel.getAllCellInfo();
            int lastIndex = infoList != null && infoList.size() > 0 ? infoList.size() - 1 : -1;
            if (lastIndex > -1) {
                CellInfo info = infoList.get(lastIndex);
                if (info instanceof CellInfoGsm) {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                    return new CellTowerInfo(
                            "GSM",
                            "" + identityGsm.getCid(),
                            "" + gsm.getDbm(),
                            "" + identityGsm.getLac());
                } else if (info instanceof CellInfoLte) {
                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    return new CellTowerInfo(
                            "LTE",
                            "" + identityLte.getCi(),
                            "" + lte.getDbm(),
                            "" + identityLte.getTac());
                }
            }
        }
        return null;
    }

    private static final int INTERNET_CHECKING_INTERVAL = 20 * 1000;
    private static final int MAX_INTERNET_CHECKING_INTERVAL = 2 * 60 * 1000;
    private static final int INTERVAL_DOUBLE_AT = 2 * 60 * 1000;
    private static ConnectivityManager _connectivityManager;
    private static ConnectivityManager.NetworkCallback _networkCallback;
    private static Handler _handler;
    private static final MutableLiveData<Boolean> _connectionStatus = new MutableLiveData<>(false);

    public static LiveData<Boolean> getLiveConnectionStatus() {
        return _connectionStatus;
    }

    public static void startConnectionStatusUpdates(Context ctx, String url) {
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
                    Log.i(TAG, "onAvailable: ");
                    if (_handler == null) {
                        _handler = new Handler();
                    }

                    long elapsedTime = System.currentTimeMillis();
                    final int[] currentInterval = {INTERNET_CHECKING_INTERVAL};
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (currentInterval[0] <= MAX_INTERNET_CHECKING_INTERVAL) {
                                if (System.currentTimeMillis() - elapsedTime >= INTERVAL_DOUBLE_AT) {
                                    currentInterval[0] *= 2;
                                }
                            }
                            Boolean isReachable = isReachable(ctx, url);
                            Log.i(TAG, (isReachable ? "" : "not") + " connected to the internet.");
                            _connectionStatus.postValue(isReachable);
                            _handler.postDelayed(this, currentInterval[0]);
                        }
                    };

                    _handler.post(runnable);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    Log.i(TAG, "onLost: ");
                    _connectionStatus.postValue(false);
                    if (_handler != null) {
                        _handler.removeCallbacksAndMessages(null);
                    }
                }
            };

            _connectivityManager = ctx.getSystemService(ConnectivityManager.class);
            _connectivityManager.requestNetwork(networkRequest, _networkCallback);
        } catch (Exception e) {
            Log.e(TAG, "startConnectionStatusUpdates: ", e);
        }
    }

    public static void stopConnectionStatusUpdates() {
        if (_connectivityManager != null && _networkCallback != null) {
            _connectivityManager.unregisterNetworkCallback(_networkCallback);
            if (_handler != null) {
                _handler.removeCallbacksAndMessages(null);
            }
        }
    }

    public static CompletableFuture<Boolean> isReachableAsync(String url, int timeout) {
        return CompletableFuture.supplyAsync(() -> {
            TrafficStats.setThreadStatsTag(NETWORK_UTIL_CONNECTION_TAG); // Tagging the current thread
            try {
                URL connectionUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) connectionUrl.openConnection();
                connection.setConnectTimeout(timeout);
                connection.connect();
                return isResponseSuccessful(connection.getResponseCode());
            } catch (Exception e) {
                return false;
            } finally {
                TrafficStats.clearThreadStatsTag(); // Clear the tag after the network operation is done
            }
        });
    }

    public static boolean isResponseSuccessful(int responseCode) {
        return responseCode >= 200 && responseCode < 300;
    }

    public static boolean isReachable(Context context, String url) {
        return isReachable(context, url, 5 * 1000);
    }

    public static boolean isReachable(Context context, String url, int timeout) {
        try {
            if (!isConnected(context)) {
                return false;
            }

            URL connectionUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) connectionUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static class CellTowerInfo {
        public String type;
        public String towerId;
        public String signalStrength;
        public String lac;

        public CellTowerInfo(String type, String towerId, String signalStrength, String lac) {
            this.type = type;
            this.towerId = towerId;
            this.signalStrength = signalStrength;
            this.lac = lac;
        }
    }

    private static WifiInfo getWifiInfo(Context context) {
        return ((WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE))
                .getConnectionInfo();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }

    public static CompletableFuture<String> getPublicIpAddress() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.ipify.org");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String ipAddress = reader.readLine();
                    Log.i(TAG, "Public IP Address: " + ipAddress); // Log the IP address
                    return ipAddress;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch public IP address", e);
            }
            return "0.0.0.0";
        });
    }


}
