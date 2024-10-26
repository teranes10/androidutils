package com.github.teranes10.androidutils.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class DeviceUtil {
    private static final String TAG = "DeviceUtil";

    public static String getDeviceModel() {
        return Build.MODEL;
    }

    public static String getDeviceOS() {
        return Build.VERSION.RELEASE;
    }

    @SuppressLint("HardwareIds")
    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static void setBrightness(Activity context, int percentage) {
        Window w = context.getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = (float) percentage / 100;
        w.setAttributes(lp);
    }

    public static List<AppInfo> getInstalledApps(Context context) {
        List<AppInfo> AppInfoList = new ArrayList<>();

        try {
            PackageManager pm = context.getPackageManager();
            @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            @SuppressLint("SimpleDateFormat") Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

            for (ApplicationInfo app : apps) {
                PackageInfo packageInfo = pm.getPackageInfo(app.packageName, 0);
                AppInfo info = new AppInfo();

                info.name = (String) pm.getApplicationLabel(app);
                info.packageName = packageInfo.packageName;
                info.version = packageInfo.versionName;
                info.minSDK = app.minSdkVersion;
                info.targetSDK = app.targetSdkVersion;
                info.firstInstallTime = formatter.format(new Date(packageInfo.firstInstallTime));
                info.lastUpdateTime = formatter.format(new Date(packageInfo.lastUpdateTime));
                info.isSystemApp = (app.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
                if (packageInfo.activities != null) {
                    info.activities = Arrays.stream(packageInfo.activities).map(x -> x.name)
                            .reduce((result, element) -> result + ", " + element).orElse("");
                }
                if (packageInfo.receivers != null) {
                    info.receivers = Arrays.stream(packageInfo.receivers).map(x -> x.name)
                            .reduce((result, element) -> result + ", " + element).orElse("");
                }
                AppInfoList.add(info);
            }
        } catch (Exception e) {
            Log.e(TAG, "getInstalledApps: ", e);
        }

        return AppInfoList;
    }

    public static Boolean isAppAlreadyInstalled(Context context, String packageName, String versionName) {
        try {
            PackageManager pm = context.getPackageManager();
            @SuppressLint("QueryPermissionsNeeded") List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            for (ApplicationInfo app : apps) {
                if (!((app.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0)) {
                    PackageInfo packageInfo = pm.getPackageInfo(app.packageName, 0);
                    if ((app.packageName.trim().equalsIgnoreCase(packageName.trim())) &&
                            (packageInfo.versionName.trim().equalsIgnoreCase(versionName.trim()))) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String _runningApp = null;

    public static String getRunningApp(Context ctx) {
        List<UsageStats> stats = getUsageStats(ctx, 5000);
        if (stats.isEmpty()) {
            return _runningApp;
        }

        _runningApp = stats.get(0).getPackageName();
        return _runningApp;
    }

    public static List<UsageStats> getUsageStats(Context ctx, long interval) {
        long currentTime = System.currentTimeMillis();
        UsageStatsManager usm = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - interval, currentTime);
        if (usageStatsList == null || usageStatsList.isEmpty()) {
            return new ArrayList<>();
        }

        SortedMap<Long, UsageStats> sortedStatsMap = new TreeMap<>((a, b) -> Long.compare(b, a)); // Reverse order
        for (UsageStats usageStats : usageStatsList) {
            sortedStatsMap.put(usageStats.getLastTimeUsed(), usageStats);
        }

        return new ArrayList<>(sortedStatsMap.values());
    }

    public static class AppInfo {
        public String name;
        public String packageName;
        public String version;
        public Integer minSDK;
        public Integer targetSDK;
        public String firstInstallTime;
        public String lastUpdateTime;
        public String activities;
        public String receivers;
        public Boolean isSystemApp;
    }
}
