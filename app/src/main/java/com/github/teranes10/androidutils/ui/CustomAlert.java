package com.github.teranes10.androidutils.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.github.teranes10.androidutils.R;

public class CustomAlert extends CustomAlertBase<CustomAlert> {
    private final Context _ctx;
    private final Dialog _dialog;

    public interface CustomAlertEvent {
        void onCreate(Dialog dialog);
    }

    public CustomAlert(Activity context) {
        this(context, null, null);
    }

    public CustomAlert(Context context, Integer themeId) {
        this(context, themeId, null);
    }

    public CustomAlert(Context context, Integer themeId, CustomAlertEvent listener) {
        _ctx = context;
        _dialog = themeId != null ? new Dialog(context, themeId) : new Dialog(context);
        _dialog.setContentView(R.layout.custom_alert);
        _dialog.setCancelable(false);

        Window window = _dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.y = 150;
            params.flags &= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(params);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (listener != null) {
            listener.onCreate(_dialog);
        }

        bindView(context, _dialog.findViewById(android.R.id.content));
    }

    @Override
    public boolean show() {
        if (_dialog != null && !_dialog.isShowing()) {
            _dialog.show();

            if (hasTextField() && _ctx instanceof Activity) {
                SoftKeyboard.showKeyboard(_ctx, getTextField());
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean isShowing() {
        return _dialog != null && _dialog.isShowing();
    }

    @Override
    public void close() {
        if (isShowing()) {
            _dialog.dismiss();

            if (hasTextField() && _ctx instanceof Activity) {
                SoftKeyboard.hideKeyboard(_ctx, getTextField());
            }
        }
    }
}
