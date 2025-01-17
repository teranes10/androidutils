package com.github.teranes10.androidutils.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class TextListener {

    public interface OnTextChangeListener {
        void onTextChanged(String val, String oldVal);
    }

    public interface OnTextChangeListener2 {
        void onTextChanged(EditText v, String val, String oldVal);
    }

    public static void setOnTextChangeListener(EditText field, long delay, OnTextChangeListener listener) {
        setOnTextChangeListener(field, false, delay, listener);
    }

    public static void setOnTextChangeListener(EditText field, boolean setOnly, long delay, OnTextChangeListener listener) {
        Handler handler = new Handler(Looper.getMainLooper());

        field.addTextChangedListener(new TextWatcher() {
            String oldVal = "";
            String val = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                oldVal = s.toString().trim();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                val = s.toString().trim();
                boolean isOld = val.equals(oldVal);
                boolean isSet = val.length() - oldVal.length() > 1;
                handler.removeCallbacksAndMessages(null);

                if (!isOld) {
                    if (isSet) {
                        listener.onTextChanged(val, oldVal);
                    } else if (!setOnly) {
                        handler.postDelayed(() -> listener.onTextChanged(val, oldVal), delay);
                    }
                }
            }
        });
    }

    public static void setOnTextChangeListener(Activity activity, long delay, OnTextChangeListener2 listener) {
        setOnTextChangeListener(activity, false, delay, listener);
    }

    public static void setOnTextChangeListener(Activity activity, boolean setOnly, long delay, OnTextChangeListener2 listener) {
        if (activity == null || activity.getWindow() == null || activity.getWindow().getDecorView() == null) {
            return;
        }

        getAllEditText(activity.getWindow().getDecorView()).forEach(view ->
                setOnTextChangeListener(view, setOnly, delay, (val, oldVal) -> listener.onTextChanged(view, val, oldVal)));
    }

    public static List<EditText> getAllEditText(View view) {
        List<EditText> editTexts = new ArrayList<>();

        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                editTexts.addAll(getAllEditText(group.getChildAt(i)));
            }
        } else if (view instanceof EditText) {
            editTexts.add((EditText) view);
        }

        return editTexts;
    }
}
