package com.github.teranes10.androidutils.utils;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

    public static void showKeyboard(Context ctx, EditText editText) {
        new Handler().post(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) ctx.getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(editText.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            editText.requestFocus();
        });
    }

    public static void hideKeyboard(Activity activity) {
        try {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "hideKeyboard: " + e.getLocalizedMessage());
        }
    }

    public static boolean isDoubleClick(View v) {
        TranslateAnimation animation = new TranslateAnimation(-50.0f, 0.0f, 0.0f, 0.0f);
        animation.setDuration(50);
        animation.setFillAfter(false);
        animation.setRepeatCount(2);
        animation.setRepeatMode(2);
        v.startAnimation(animation);

        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_ALARM, 60);
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 10);

        return isDoubleClick();
    }

    public static long lastClickTime = 0;
    public static final long DOUBLE_CLICK_TIME_DELTA = 750;

    public static boolean isDoubleClick() {
        long clickTime = System.currentTimeMillis();
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            return true;
        }

        lastClickTime = clickTime;
        return false;
    }

    public static void goToLocationSettings(Activity context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivityForResult(intent, 0, null);
    }

    public static int getSeconds() {
        return Calendar.getInstance().get(Calendar.SECOND);
    }
}
