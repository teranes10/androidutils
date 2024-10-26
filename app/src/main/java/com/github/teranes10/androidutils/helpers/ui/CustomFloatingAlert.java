package com.github.teranes10.androidutils.helpers.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.teranes10.androidutils.R;
import com.github.teranes10.androidutils.utils.AndroidUtil;
import com.github.teranes10.androidutils.utils.Utils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class CustomFloatingAlert {
    private FloatingAlert _dialog;
    private ImageView _imageView;
    private TextView _textView;
    private TextInputEditText _text_field;
    private TextInputLayout _text_layout;
    private Button _negativeBtn, _positiveBtn;
    private onPositiveClickListener _positiveClickListener;
    private onNegativeClickListener _negativeClickListener;

    public enum AlertType {
        Error, Warning, Emergency, Success, IncomingMessage, Info
    }

    public interface onPositiveClickListener {
        void onPositiveClick(CustomFloatingAlert dialog);
    }

    public interface onNegativeClickListener {
        void onNegativeClick(CustomFloatingAlert dialog);
    }

    public CustomFloatingAlert setPositiveBtn(String text, onPositiveClickListener listener) {
        _positiveBtn.setText(text);
        _positiveClickListener = listener;

        return this;
    }

    public CustomFloatingAlert setNegativeBtn(String text, onNegativeClickListener listener) {
        _negativeBtn.setVisibility(View.VISIBLE);
        _negativeBtn.setText(text);
        _negativeClickListener = listener;

        return this;
    }

    public CustomFloatingAlert setAlertType(AlertType type) {
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
                // _positiveBtn.setBackgroundColor(Color.parseColor("#25b7d3"));
                _positiveBtn.setBackgroundColor(Color.parseColor("#FF03DAC5"));
                break;
        }

        _imageView.setVisibility(View.VISIBLE);
        return this;
    }

    public CustomFloatingAlert setMessage(String message) {
        _textView.setText(message);
        return this;
    }

    public CustomFloatingAlert setMessage(String message, int alignment, float size) {
        _textView.setText(message);
        _textView.setTextAlignment(alignment);
        _textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        return this;
    }

    public CustomFloatingAlert setNoActions() {
        _positiveBtn.setVisibility(View.GONE);
        _negativeBtn.setVisibility(View.GONE);
        return this;
    }

    public CustomFloatingAlert(Context context) {
        create(context, null);
    }

    public CustomFloatingAlert(Context context, AlertView listener) {
        create(context, listener);
    }

    private void create(Context context, AlertView listener) {
        _dialog = new FloatingAlert(context, R.layout.custom_alert);
        if (listener != null) {
            _dialog = listener.onCreate(_dialog);
        }

        _dialog.onViewCreated((view, alert) -> {
            _imageView = view.findViewById(R.id.alert_image);
            _textView = view.findViewById(R.id.alert_message);
//            _text_layout = view.findViewById(R.id.alert_text_layout);
//            _text_field = view.findViewById(R.id.alert_text_field);
            _negativeBtn = view.findViewById(R.id.alert_negative_btn);
            _positiveBtn = view.findViewById(R.id.alert_positive_btn);

            _negativeBtn.setOnClickListener(v1 -> {
                if (AndroidUtil.isDoubleClick(v1)) {
                    return;
                }
                if (_negativeClickListener != null) {
                    _negativeClickListener.onNegativeClick(CustomFloatingAlert.this);
                } else {
                    _dialog.close();
                }
            });


            _positiveBtn.setOnClickListener(v2 -> {
                if (AndroidUtil.isDoubleClick(v2)) {
                    return;
                }
                if (_positiveClickListener != null) {
                    _positiveClickListener.onPositiveClick(CustomFloatingAlert.this);
                } else {
                    _dialog.close();
                }
            });

            setAlertType(AlertType.Info);
        });

        _dialog.build();
    }

    public CustomFloatingAlert setTextFieldHint(String hintString) {
        _imageView.setVisibility(View.GONE);
        _text_layout.setVisibility(View.VISIBLE);
        _text_field.setHint(hintString);
        return this;
    }

    public CustomFloatingAlert setTextField(String val) {
        _imageView.setVisibility(View.GONE);
        _text_layout.setVisibility(View.VISIBLE);
        _text_field.setText(val);
        return this;
    }

    public CustomFloatingAlert setTextFieldType(int type) {
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
            _dialog.close();
        }
    }

    public boolean isShowing() {
        return _dialog.isShowing();
    }

    public interface AlertView {
        FloatingAlert onCreate(FloatingAlert alert);
    }
}
