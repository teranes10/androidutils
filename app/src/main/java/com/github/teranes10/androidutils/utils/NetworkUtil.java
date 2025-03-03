package com.github.teranes10.androidutils.utils;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

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
    private static final int NETWORK_UTIL_CONNECTION_TAG = 123123;
    private static final String TAG = "NetworkUtil";

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }

        return false;
    }

    public static String getSSID(WifiInfo wifiInfo) {
        return wifiInfo.getSSID();
    }

    public static String getBSSID(WifiInfo wifiInfo) {
        return wifiInfo.getBSSID();
    }

    public static String getLinkSpeed(WifiInfo wifiInfo) {
        return wifiInfo.getLinkSpeed() + "Mbps";
    }

    public static String getIpAddress(WifiInfo wifiInfo) {
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

    public static String getSignalStrength(WifiInfo wifiInfo) {
        @SuppressLint("DefaultLocale") String RSSI = String.format("%d", wifiInfo.getRssi());
        return RSSI + "dB";
    }

    public static CellTowerInfo getCellInfo(Context ctx) {
        TelephonyManager tel = (TelephonyManager) ctx.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        List<CellInfo> infoList = tel.getAllCellInfo();
        if (infoList == null || infoList.isEmpty()) {
            return null;
        }

        for (CellInfo info : infoList) {
            if (!info.isRegistered()) continue;

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                if (info instanceof CellInfoGsm cellInfoGsm) {
                    CellIdentityGsm identity = cellInfoGsm.getCellIdentity();
                    return createCellTowerInfo("GSM", identity, cellInfoGsm.getCellSignalStrength(), -1);
                }
                if (info instanceof CellInfoLte cellInfoLte) {
                    CellIdentityLte identity = cellInfoLte.getCellIdentity();
                    return createCellTowerInfo("LTE", identity, cellInfoLte.getCellSignalStrength(), identity.getPci());
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= 30 && info instanceof CellInfoNr cellInfoNr) {
                CellIdentityNr identity = (CellIdentityNr) cellInfoNr.getCellIdentity();
                return createCellTowerInfo("NR", identity, cellInfoNr.getCellSignalStrength(), identity.getPci());
            }
        }

        return null;
    }

    private static CellTowerInfo createCellTowerInfo(String type, CellIdentity identity, CellSignalStrength signalStrength, int pci) {
        String mcc = "", mnc = "";
        int id = -1, area = -1;

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            if (identity instanceof CellIdentityGsm gsm) {
                mcc = gsm.getMccString() != null ? gsm.getMccString() : "";
                mnc = gsm.getMncString() != null ? gsm.getMncString() : "";
                id = gsm.getCid();
                area = gsm.getLac();
            } else if (identity instanceof CellIdentityLte lte) {
                mcc = lte.getMccString() != null ? lte.getMccString() : "";
                mnc = lte.getMncString() != null ? lte.getMncString() : "";
                id = lte.getCi();
                area = lte.getTac();
            } else if (android.os.Build.VERSION.SDK_INT >= 30 && identity instanceof CellIdentityNr nr) {
                mcc = nr.getMccString() != null ? nr.getMccString() : "";
                mnc = nr.getMncString() != null ? nr.getMncString() : "";
                id = (int) nr.getNci();
                area = nr.getTac();
            }
        }

        return new CellTowerInfo(type, String.valueOf(id), String.valueOf(signalStrength.getDbm()), String.valueOf(area), mcc, mnc, pci);
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
        }).exceptionally(e -> false);
    }

    public static boolean isResponseSuccessful(int responseCode) {
        return responseCode >= 200 && responseCode < 300;
    }

    public static class CellTowerInfo {
        public String type;
        public String towerId;
        public String signalStrength;
        public String lac;

        public String mnc;
        public String mcc;
        public int pci;

        public CellTowerInfo(String type, String towerId, String signalStrength, String lac, String mcc, String mnc, int pci) {
            this.type = type;
            this.towerId = towerId;
            this.signalStrength = signalStrength;
            this.lac = lac;
            this.mnc = mnc;
            this.mcc = mcc;
            this.pci = pci;
        }

        public CellTowerInfo(String type, String towerId, String signalStrength, String lac) {
            this.type = type;
            this.towerId = towerId;
            this.signalStrength = signalStrength;
            this.lac = lac;
        }

        @Override
        public String toString() {
            return "CellTowerInfo{" +
                    "type='" + type + '\'' +
                    ", towerId='" + towerId + '\'' +
                    ", signalStrength='" + signalStrength + '\'' +
                    ", lac='" + lac + '\'' +
                    ", mnc='" + mnc + '\'' +
                    ", mcc='" + mcc + '\'' +
                    ", pci=" + pci +
                    '}';
        }
    }

    public static WifiInfo getWifiInfo(Context context) {
        return ((WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE))
                .getConnectionInfo();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
