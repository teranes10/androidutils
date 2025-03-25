package com.github.teranes10.androidutils.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

import com.github.teranes10.androidutils.R;

public class Loading {
    private final Activity context;
    private Dialog dialog;

    public Loading(Activity context) {
        this.context = context;
        init(context);
    }

    private void init(Context context) {
        dialog = new Dialog(context, R.style.FullScreenAlert);
        dialog.setContentView(R.layout.custom_loading);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public void start(String message) {
        if (context == null || context.isFinishing() || context.isDestroyed()) {
            return;
        }

        if (dialog.isShowing()) {
            return;
        }

        TextView textView = dialog.findViewById(R.id.loading_text);
        if (textView != null) {
            textView.setText(message != null ? message : "");
        }

        dialog.show();
    }

    public void start() {
        start("");
    }

    public void stop() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}

