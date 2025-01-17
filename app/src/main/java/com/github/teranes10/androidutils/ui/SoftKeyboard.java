package com.github.teranes10.androidutils.ui;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class SoftKeyboard {
    private static final String TAG = "SoftKeyboard";

    public static void showKeyboard(Context ctx, EditText editText) {
        new Handler(Looper.getMainLooper()).post(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) ctx.getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(editText.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
            editText.requestFocus();
        });
    }

    public static void hideKeyboard(Context ctx, EditText editText) {
        new Handler(Looper.getMainLooper()).post(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) ctx.getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(editText.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, null);
            editText.requestFocus();
        });
    }

    public static void hideKeyboard(Activity activity) {
        try {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "hideKeyboard: " + e.getLocalizedMessage());
        }
    }
}
