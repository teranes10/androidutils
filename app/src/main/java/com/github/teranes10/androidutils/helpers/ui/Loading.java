package com.github.teranes10.androidutils.helpers.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

import com.github.teranes10.androidutils.R;

public class Loading {
    private static Dialog dialog;

    private static void init(Context context) {
        dialog = new Dialog(context, R.style.FullScreenAlert);
        dialog.setContentView(R.layout.custom_loading);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public static void start(Context context) {
        if (dialog == null) {
            init(context);
        }

        TextView textView = dialog.findViewById(R.id.loading_text);
        textView.setText("");
        dialog.show();
    }

    public static void start(Activity context, String message) {
        if(context.hasWindowFocus()){
            return;
        }
        if(context.isFinishing()){
            return;
        }

        if (dialog == null) {
            init(context);
        }


    }

    public static void stop() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}

