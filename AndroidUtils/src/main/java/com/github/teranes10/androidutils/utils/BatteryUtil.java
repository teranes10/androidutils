package com.github.teranes10.androidutils.utils;

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import androidx.annotation.NonNull;

public class BatteryUtil extends EventUtil<BatteryUtil.BatteryStatusListener> {
    private static final String TAG = "BatteryUtil";
    private static BatteryUtil instance;
    private long lastChargerConnectedTime = -1;
    private boolean isChargerConnected = false;
    private boolean isLowBattery = false;
    private static final int CRITICAL_BATTERY_LOW_LEVEL = 10;

    private final BroadcastReceiver batteryStatusChangesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryLevel = (int) (((float) level / (float) scale) * 100.0f);

            String statusString = getBatteryStatus(status);
            BatteryStatus batteryStatus = new BatteryStatus(batteryLevel, status, statusString);
            if (batteryStatus.status.equals(STATUS_UNKNOWN)) {
                return;
            }

            notifyListeners(listener -> {
                listener.onBatteryStatusChanged(batteryStatus);

                if (batteryStatus.status.equals(STATUS_CHARGING) && !isChargerConnected) {
                    lastChargerConnectedTime = System.currentTimeMillis();
                    isChargerConnected = true;
                    notifyListeners(BatteryStatusListener::onChargerConnected);
                } else if (batteryStatus.status.equals(STATUS_DISCHARGING) && isChargerConnected) {
                    isChargerConnected = false;
                    notifyListeners(BatteryStatusListener::onChargerDisconnected);
                }

                boolean temp_isLow = batteryLevel <= CRITICAL_BATTERY_LOW_LEVEL;
                if (temp_isLow && !isLowBattery) {
                    isLowBattery = true;
                    notifyListeners(BatteryStatusListener::onLowBattery);
                } else if (!temp_isLow && isLowBattery) {
                    isLowBattery = false;
                }
            }, 5 * 1000);
        }
    };

    public long getChargerConnectedTime() {
        return lastChargerConnectedTime;
    }

    public static synchronized BatteryUtil getInstance() {
        if (instance == null) {
            instance = new BatteryUtil();
        }

        return instance;
    }

    public static void setListener(Context context, BatteryStatusListener listener) {
        BatteryUtil instance = getInstance();
        if (instance.addListener(listener) && instance.getListenerCount() == 1) {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(instance.batteryStatusChangesReceiver, intentFilter, RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(instance.batteryStatusChangesReceiver, intentFilter);
            }
        }
    }

    public static void removeListener(Context context, BatteryStatusListener listener) {
        if (instance == null) {
            return;
        }

        if (instance.removeListener(listener) && instance.getListenerCount() == 0) {
            context.unregisterReceiver(instance.batteryStatusChangesReceiver);
        }
    }

    public static final String STATUS_NOT_CHARGING = "Not Charging";
    public static final String STATUS_CHARGING = "Charging";
    public static final String STATUS_FULL = "Full";
    public static final String STATUS_DISCHARGING = "Discharging";
    public static final String STATUS_UNKNOWN = "Unknown";

    private static String getBatteryStatus(int status) {
        return switch (status) {
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING -> STATUS_NOT_CHARGING;
            case BatteryManager.BATTERY_STATUS_CHARGING -> STATUS_CHARGING;
            case BatteryManager.BATTERY_STATUS_FULL -> STATUS_FULL;
            case BatteryManager.BATTERY_STATUS_DISCHARGING -> STATUS_DISCHARGING;
            default -> STATUS_UNKNOWN;
        };
    }

    public static class BatteryStatus {
        public int level;
        public int type;
        public String status;

        public BatteryStatus(int level, int type, String status) {
            this.level = level;
            this.type = type;
            this.status = status;
        }

        @NonNull
        @Override
        public String toString() {
            return "BatteryStatus{" +
                    "level=" + level +
                    ", type=" + type +
                    ", status='" + status + '\'' +
                    '}';
        }
    }

    public interface BatteryStatusListener {
        void onBatteryStatusChanged(BatteryStatus status);

        void onChargerConnected();

        void onChargerDisconnected();

        void onLowBattery();
    }
}
