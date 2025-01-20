package com.github.teranes10.androidutils.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class AppUtil {
    private static final String TAG = "AppUtil";

    public static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo;
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0.0.0.0";
        }
    }

    private static Boolean isVersionUpToDate(String deviceVersion, String serverVersion) {
        try {
            int[] deviceVersionComponents = Arrays.stream(deviceVersion.replaceAll("[^0-9.]", "").split("\\."))
                    .mapToInt(Integer::parseInt).toArray();
            int[] serverVersionComponents = Arrays.stream(serverVersion.replaceAll("[^0-9.]", "").split("\\."))
                    .mapToInt(Integer::parseInt).toArray();

            int deviceYear = deviceVersionComponents[0];
            int deviceMonth = deviceVersionComponents[1];
            int deviceDay = deviceVersionComponents[2];
            int deviceIncrement = deviceVersionComponents[3];

            int serverYear = serverVersionComponents[0];
            int serverMonth = serverVersionComponents[1];
            int serverDay = serverVersionComponents[2];
            int serverIncrement = serverVersionComponents[3];

            return !(serverYear > deviceYear || (serverYear == deviceYear && serverMonth > deviceMonth)
                    || (serverYear == deviceYear && serverMonth == deviceMonth && serverDay > deviceDay)
                    || (serverYear == deviceYear && serverMonth == deviceMonth && serverDay == deviceDay && serverIncrement > deviceIncrement));
        } catch (Exception e) {
            Log.e(TAG, "versionChecker: " + e.getLocalizedMessage());
        }

        return false;
    }

    public static Boolean isVersionUpToDate(Context context, String serverVersion, boolean force) {
        String deviceVersion = getAppVersion(context);
        if (force) {
            return serverVersion.equals(deviceVersion);
        }

        return isVersionUpToDate(deviceVersion, serverVersion);
    }

    public static Boolean isVersionUpToDate(Context context, String serverVersion) {
        return isVersionUpToDate(context, serverVersion, false);
    }

    public static boolean isAppRunning(Context context, String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfo_list = activityManager.getRunningAppProcesses();
        if (procInfo_list != null) {
            ActivityManager.RunningAppProcessInfo processInfo = procInfo_list.stream()
                    .filter(x -> x.processName.equals(packageName)).findAny().orElse(null);
            return processInfo != null;
        }

        return false;
    }

    public static void installAPK(Context context, File file) {
        try {
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(FileProvider.getUriForFile(context,
                                context.getPackageName() + ".provider", file),
                        "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Thread thread = new Thread(() -> {
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
                Thread.sleep(100);
            }
        } catch (Exception e) {
            Log.e(TAG, "installAPK: ", e);
        }
    }
}
