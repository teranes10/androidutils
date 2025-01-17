package com.github.teranes10.androidutils.utils;

import static android.content.Context.RECEIVER_EXPORTED;

import static com.github.teranes10.androidutils.utils.BatteryUtil.BatteryStatus.STATUS_CHARGING;
import static com.github.teranes10.androidutils.utils.BatteryUtil.BatteryStatus.STATUS_DISCHARGING;
import static com.github.teranes10.androidutils.utils.BatteryUtil.BatteryStatus.STATUS_UNKNOWN;
import static com.github.teranes10.androidutils.utils.BatteryUtil.BatteryStatus.getBatteryStatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

public class BatteryUtil extends EventUtil<BatteryUtil.BatteryStatusListener> {
    private int criticalBatteryLowLevel = 10;
    private long lastChargerConnectedTime = -1;
    private boolean isChargerConnected = false;
    private boolean isLowBattery = false;
    private final BroadcastReceiver batteryStatusChangesReceiver;
    private static WeakReference<BatteryUtil> instanceWeakRef;

    public BatteryUtil(Context context) {
        batteryStatusChangesReceiver = new BroadcastReceiver() {
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

                    boolean temp_isLow = batteryLevel <= criticalBatteryLowLevel;
                    if (temp_isLow && !isLowBattery) {
                        isLowBattery = true;
                        notifyListeners(BatteryStatusListener::onLowBattery);
                    } else if (!temp_isLow && isLowBattery) {
                        isLowBattery = false;
                    }
                }, 5 * 1000);
            }
        };

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getApplicationContext().registerReceiver(batteryStatusChangesReceiver, intentFilter, RECEIVER_EXPORTED);
        } else {
            context.getApplicationContext().registerReceiver(batteryStatusChangesReceiver, intentFilter);
        }
    }

    public void setCriticalBatteryLowLevel(int level) {
        this.criticalBatteryLowLevel = level;
    }

    public long getChargerConnectedTime() {
        return lastChargerConnectedTime;
    }

    @Override
    public boolean addListener(BatteryStatusListener listener) {
        return super.addListener(listener);
    }

    public synchronized boolean removeListener(Context context, BatteryStatusListener listener) {
        if (super.removeListener(listener)) {
            if (getListenerCount() == 0) {
                context.getApplicationContext().unregisterReceiver(batteryStatusChangesReceiver);
                if (instanceWeakRef != null) {
                    instanceWeakRef.clear();
                    instanceWeakRef = null;
                }
            }

            return true;
        }

        return false;
    }


    public static synchronized BatteryUtil getInstance(Context context) {
        BatteryUtil instance = instanceWeakRef != null ? instanceWeakRef.get() : null;
        if (instance == null) {
            instance = new BatteryUtil(context);
            instanceWeakRef = new WeakReference<>(instance);
        }

        return instance;
    }

    public static class BatteryStatus {
        public static final String STATUS_NOT_CHARGING = "Not Charging";
        public static final String STATUS_CHARGING = "Charging";
        public static final String STATUS_FULL = "Full";
        public static final String STATUS_DISCHARGING = "Discharging";
        public static final String STATUS_UNKNOWN = "Unknown";

        public int level;
        public int type;
        public String status;

        public BatteryStatus(int level, int type, String status) {
            this.level = level;
            this.type = type;
            this.status = status;
        }

        public static String getBatteryStatus(int status) {
            return switch (status) {
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING -> STATUS_NOT_CHARGING;
                case BatteryManager.BATTERY_STATUS_CHARGING -> STATUS_CHARGING;
                case BatteryManager.BATTERY_STATUS_FULL -> STATUS_FULL;
                case BatteryManager.BATTERY_STATUS_DISCHARGING -> STATUS_DISCHARGING;
                default -> STATUS_UNKNOWN;
            };
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
