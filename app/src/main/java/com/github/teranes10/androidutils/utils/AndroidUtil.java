package com.github.teranes10.androidutils.utils;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import java.util.Calendar;

public class AndroidUtil {
    private static final String TAG = "AndroidUtil";

    public static void toast(Context ctx, String msg) {
        runOnUI(() -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }

    public static void runOnUI(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static void goToLocationSettings(Activity context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivityForResult(intent, 0, null);
    }

    public static int getSeconds() {
        return Calendar.getInstance().get(Calendar.SECOND);
    }
}
