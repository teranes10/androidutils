package com.github.teranes10.androidutils.helpers.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.teranes10.androidutils.R;
import com.github.teranes10.androidutils.utils.AndroidUtil;
import com.github.teranes10.androidutils.utils.Utils;

import java.util.Objects;

public class CustomAlert {
    private final Dialog _dialog;
    private final ImageView _imageView;
    private final TextView _textView;
    private final EditText _text_field;
    private final Button _negativeBtn, _positiveBtn;
    private onPositiveClickListener _positiveClickListener;
    private onNegativeClickListener _negativeClickListener;
    private Context _ctx;

    public enum AlertType {
        Error, Warning, Emergency, Success, Info
    }

    public interface onPositiveClickListener {
        void onPositiveClick(CustomAlert dialog);
    }

    public interface onNegativeClickListener {
        void onNegativeClick(CustomAlert dialog);
    }

    public CustomAlert setPositiveBtn(String text, onPositiveClickListener listener) {
        _positiveBtn.setText(text);
        _positiveClickListener = listener;
        return this;
    }

    public CustomAlert setNegativeBtn(String text, onNegativeClickListener listener) {
        _negativeBtn.setVisibility(View.VISIBLE);
        _negativeBtn.setText(text);
        _negativeClickListener = listener;
        return this;
    }

    public CustomAlert setAlertType(AlertType type) {
        switch (type) {
            case Error:
                _imageView.setBackgroundResource(R.drawable.alert_error);
                _positiveBtn.setBackgroundColor(Color.parseColor("#e24c4b"));
                break;
            case Warning:
                _imageView.setBackgroundResource(R.drawable.alert_warning);
                _positiveBtn.setBackgroundColor(Color.parseColor("#ffd764"));
                break;
            case Emergency:
                _imageView.setBackgroundResource(R.drawable.alert_emergency);
                _positiveBtn.setBackgroundColor(Color.parseColor("#ffd764"));
                break;
            case Success:
                _imageView.setBackgroundResource(R.drawable.alert_success);
                _positiveBtn.setBackgroundColor(Color.parseColor("#32bea6"));
                break;
            default:
                _imageView.setBackgroundResource(R.drawable.alert_info);
                _positiveBtn.setBackgroundColor(Color.parseColor("#FF03DAC5"));
                break;
        }

        _imageView.setVisibility(View.VISIBLE);
        return this;
    }

    public CustomAlert setMessage(String message) {
        _textView.setText(message);
        return this;
    }

    public CustomAlert setMessage(String message, int alignment, float size) {
        _textView.setText(message);
        _textView.setTextAlignment(alignment);
        _textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        return this;
    }

    public CustomAlert(Context context) {
        this(context, null);
    }

    public CustomAlert(Context context, Integer themeId) {
        _ctx = context;
        _dialog = themeId != null ? new Dialog(context, themeId) : new Dialog(context);
        _dialog.setContentView(R.layout.custom_alert);

        Window window = _dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        params.y = 150;
        params.flags &= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(params);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        _dialog.setCancelable(false);

        _imageView = _dialog.findViewById(R.id.alert_image);
        _textView = _dialog.findViewById(R.id.alert_message);
        _text_field = _dialog.findViewById(R.id.alert_text_field);
        _negativeBtn = _dialog.findViewById(R.id.alert_negative_btn);
        _positiveBtn = _dialog.findViewById(R.id.alert_positive_btn);

        _negativeBtn.setOnClickListener(v1 -> {
            if (AndroidUtil.isDoubleClick(v1)) {
                return;
            }
            if (_negativeClickListener != null) {
                _negativeClickListener.onNegativeClick(this);
            } else {
                _dialog.dismiss();
            }
        });

        _positiveBtn.setOnClickListener(v2 -> {
            if (AndroidUtil.isDoubleClick(v2)) {
                return;
            }
            if (_positiveClickListener != null) {
                _positiveClickListener.onPositiveClick(this);
            } else {
                _dialog.dismiss();
            }
        });

        setAlertType(AlertType.Info);
    }

    public CustomAlert setTextFieldHint(String hintString) {
        _imageView.setVisibility(View.GONE);
        _text_field.setVisibility(View.VISIBLE);
        _text_field.setHint(hintString);
        return this;
    }

    public CustomAlert setTextField(String val) {
        _imageView.setVisibility(View.GONE);
        _text_field.setVisibility(View.VISIBLE);
        _text_field.setText(val);
        AndroidUtil.showKeyboard(_ctx, _text_field);
        return this;
    }

    public CustomAlert setTextFieldType(int type) {
        _text_field.setInputType(type);
        return this;
    }

    public String getTextFieldValue() {
        return Utils.optional(() -> Objects.requireNonNull(_text_field.getText()).toString().trim(), "");
    }

    public void show() {
        _dialog.show();
    }

    public void showWithTimer(int timer) {
        if (_dialog != null) {
            show();
            new Handler().postDelayed(this::close, timer);
        }
    }

    public void showWithTimer(Runnable runnable, boolean optionalRunnable, int timer) {
        if (_dialog != null) {
            show();
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                if (_dialog.isShowing()) {
                    close();
                    handler.post(runnable);
                }
            }, timer);
        }
    }

    public void close() {
        if (_dialog != null && _dialog.isShowing()) {
            _dialog.dismiss();
            if (_ctx instanceof Activity) {
                AndroidUtil.hideKeyboard((Activity) _ctx);
            }
        }
    }
}
