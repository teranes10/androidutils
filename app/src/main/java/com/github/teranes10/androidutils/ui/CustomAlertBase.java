package com.github.teranes10.androidutils.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.teranes10.androidutils.R;
import com.github.teranes10.androidutils.utils.Utils;

import java.util.Objects;

@SuppressWarnings("unchecked")
public abstract class CustomAlertBase<T extends CustomAlertBase<T>> {
    private Context _ctx;
    private ImageView _imageView;
    private TextView _textView;
    private LinearLayout _text_layout;
    private TextView _text_field_hint;
    private EditText _text_field;
    private Button _negativeBtn, _positiveBtn;
    private AlertClickListener<T> _positiveClickListener;
    private AlertClickListener<T> _negativeClickListener;
    private boolean _hasTextField;

    public enum AlertType {
        Error, Warning, Emergency, Success, Info
    }

    public enum AlertEventType {
        Positive, Negative
    }

    public interface AlertClickListener<T> {
        void onClick(T dialog);
    }

    public abstract void close();

    public abstract boolean show();

    public abstract boolean isShowing();

    public void show(int timer) {
        if (show()) {
            new Handler(Looper.getMainLooper()).postDelayed(this::close, timer);
        }
    }

    public void show(int timer, AlertEventType type) {
        if (show()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isShowing()) {
                    return;
                }

                if (type == AlertEventType.Positive && _positiveClickListener != null) {
                    _positiveClickListener.onClick((T) this);
                } else if (type == AlertEventType.Negative && _negativeClickListener != null) {
                    _negativeClickListener.onClick((T) this);
                } else {
                    close();
                }
            }, timer);
        }
    }

    public void show(int timer, Runnable runnable) {
        if (show()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                close();
                handler.post(runnable);
            }, timer);
        }
    }

    public T setPositiveBtn(String text, AlertClickListener<T> listener) {
        _positiveBtn.setText(text);
        _positiveClickListener = listener;
        return (T) this;
    }

    public T setNegativeBtn(String text, AlertClickListener<T> listener) {
        _negativeBtn.setVisibility(View.VISIBLE);
        _negativeBtn.setText(text);
        _negativeClickListener = listener;
        return (T) this;
    }

    public T setAlertType(AlertType type) {
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

        return (T) this;
    }

    public T setMessage(String message) {
        _textView.setText(message);
        return (T) this;
    }

    public T setMessage(String message, int alignment, float size) {
        _textView.setText(message);
        _textView.setTextAlignment(alignment);
        _textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        return (T) this;
    }

    public T setFontSize(float size) {
        _textView.setTextSize(size);
        return (T) this;
    }

    public T setTextAlignment(int alignment) {
        //View.TEXT_ALIGNMENT_CENTER
        _textView.setTextAlignment(alignment);
        return (T) this;
    }

    public T setTextFieldHint(String hintString) {
        _text_field_hint.setVisibility(View.VISIBLE);
        _text_field_hint.setText(hintString);
        _text_field.setHint(hintString);
        return (T) this;
    }

    public T setTextFieldType(int type) {
        _text_field.setInputType(type);
        return (T) this;
    }

    public T setTextField() {
        return setTextField("");
    }

    public T setTextField(String val) {
        _imageView.setVisibility(View.GONE);
        _text_layout.setVisibility(View.VISIBLE);
        _text_field.setText(val);
        _hasTextField = true;

        return (T) this;
    }

    public EditText getTextField() {
        return _text_field;
    }

    public boolean hasTextField() {
        return _hasTextField;
    }

    public String getTextFieldValue() {
        return Objects.requireNonNull(_text_field.getText()).toString().trim();
    }

    public void bindView(Context ctx, View view) {
        _ctx = ctx;
        _imageView = view.findViewById(R.id.alert_image);
        _textView = view.findViewById(R.id.alert_message);
        _text_layout = view.findViewById(R.id.alert_text_layout);
        _text_field_hint = view.findViewById(R.id.alert_text_hint);
        _text_field = view.findViewById(R.id.alert_text_field);
        _negativeBtn = view.findViewById(R.id.alert_negative_btn);
        _positiveBtn = view.findViewById(R.id.alert_positive_btn);

        ClickListener.setOnClickListener(_negativeBtn, v -> {
            if (_negativeClickListener != null) {
                _negativeClickListener.onClick((T) this);
            } else {
                close();
            }
        });

        ClickListener.setOnClickListener(_positiveBtn, v -> {
            if (_positiveClickListener != null) {
                _positiveClickListener.onClick((T) this);
            } else {
                close();
            }
        });

        setAlertType(AlertType.Info);
    }
}
