package com.github.teranes10.androidutils.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class MyApplication extends Application {
    private static boolean isFirstBoot;

    @Override
    public void onCreate() {
        super.onCreate();

        initializeFirstBootFlag();
    }

    private synchronized void initializeFirstBootFlag() {
        isFirstBoot = isFirstBoot(this);
        if (isFirstBoot) {
            storeFirstBoot(this);
        }
    }

    public static synchronized boolean isFirstBoot() {
        return isFirstBoot;
    }

    public void setupDefaultConnectionUtil(String url, int intervalInMillis) {
        DefaultConnectionUtil.setup(url, intervalInMillis);
    }

    private static void storeFirstBoot(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences("FIRST_BOOT", Context.MODE_PRIVATE).edit();
        editor.putBoolean("isFirstBoot", false);
        editor.apply();
    }

    private static boolean isFirstBoot(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("FIRST_BOOT", Context.MODE_PRIVATE);
        return preferences.getBoolean("isFirstBoot", true);
    }
}
