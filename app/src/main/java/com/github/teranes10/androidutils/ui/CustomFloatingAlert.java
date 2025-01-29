package com.github.teranes10.androidutils.ui;

import android.content.Context;
import android.view.Gravity;

import com.github.teranes10.androidutils.R;

public class CustomFloatingAlert extends CustomAlertBase<CustomFloatingAlert> {
    private Context _ctx;
    private FloatingAlert _dialog;

    public interface CustomFloatingAlertEvent {
        void onCreate(FloatingAlert floatingAlert);
    }

    public CustomFloatingAlert(Context context) {
        this(context, null);
    }

    public CustomFloatingAlert(Context context, CustomFloatingAlertEvent listener) {
        _ctx = context;
        _dialog = new FloatingAlert(context, R.layout.custom_alert);
        _dialog.setPosition(Gravity.CENTER, 0, 0);

        if (listener != null) {
            listener.onCreate(_dialog);
        }

        _dialog.build();
        bindView(context, _dialog.getView());
    }

    @Override
    public boolean show() {
        return _dialog.show();
    }

    @Override
    public boolean isShowing() {
        return _dialog.isShowing();
    }

    @Override
    public void close() {
        _dialog.close();
    }
}
