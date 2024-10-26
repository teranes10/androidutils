package com.github.teranes10.androidutils.helpers.ui;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.github.teranes10.androidutils.utils.PermissionUtil;

public class FloatingAlert {
    private boolean _fullscreen = false;
    private boolean _draggable = true;
    private int _width = 0;
    private int _height = 0;
    private int _x = 0;
    private int _y = 0;
    private int _gravity = Gravity.CENTER;
    private View floatView;
    private WindowManager.LayoutParams floatWindowLayoutParam;
    private WindowManager windowManager;
    private OnViewCreated _onViewCreated;
    private OnViewBinding _onViewBinding;
    private OnClickListener _onClickListener;
    private OnClickListener _onDoubleClickListener;
    private final Context _ctx;
    private static final String TAG = "FloatingAlert";

    public FloatingAlert(Context context, int layout) {
        _ctx = context;
        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        floatView = inflater.inflate(layout, null);
    }

    public FloatingAlert(Context context) {
        _ctx = context;
    }

    public FloatingAlert setDraggable(boolean draggable) {
        _draggable = draggable;
        return this;
    }

    public FloatingAlert setFullScreen(boolean fullScreen) {
        _fullscreen = fullScreen;
        return this;
    }

    public FloatingAlert bind() {
        windowManager = (WindowManager) _ctx.getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) _ctx.getSystemService(LAYOUT_INFLATER_SERVICE);
        floatView = _onViewBinding.onViewCreate(inflater, this);
        return this;
    }

    public FloatingAlert setWidth(int width) {
        _width = width;
        return this;
    }

    public FloatingAlert setHeight(int height) {
        _height = height;
        return this;
    }

    public FloatingAlert setPosition(int gravity, int x, int y) {
        _gravity = gravity;
        _x = x;
        _y = y;
        return this;
    }

    public FloatingAlert build() {
        int LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        if (_fullscreen) {
            floatWindowLayoutParam = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            floatWindowLayoutParam = new WindowManager.LayoutParams(
                    (_width > 0 ? (int) (_width * (0.55f)) : _width == 0 ? WindowManager.LayoutParams.WRAP_CONTENT : _width),
                    (_height > 0 ? (int) (_height * (0.58f)) : _height == 0 ? WindowManager.LayoutParams.WRAP_CONTENT : _height),
                    LAYOUT_TYPE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
            );
        }

        floatWindowLayoutParam.gravity = _gravity;
        floatWindowLayoutParam.x = _x;
        floatWindowLayoutParam.y = _y;

        if (_onViewCreated != null) {
            _onViewCreated.onViewCreated(floatView, this);
        }

        if (_draggable) {
            floatView.setOnTouchListener(new View.OnTouchListener() {
                final WindowManager.LayoutParams floatWindowLayoutUpdateParam = floatWindowLayoutParam;
                double x;
                double y;
                double px;
                double py;

                private final GestureDetector gestureDetector = new GestureDetector(_ctx, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                        if (_onClickListener != null) {
                            _onClickListener.onClick(FloatingAlert.this);
                        }
                        return super.onSingleTapConfirmed(e);
                    }

                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {
                        if (_onDoubleClickListener != null) {
                            _onDoubleClickListener.onClick(FloatingAlert.this);
                        }
                        return super.onDoubleTap(e);
                    }
                });

                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    gestureDetector.onTouchEvent(event);

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN -> {
                            x = floatWindowLayoutUpdateParam.x;
                            y = floatWindowLayoutUpdateParam.y;
                            px = event.getRawX();
                            py = event.getRawY();
                        }
                        case MotionEvent.ACTION_MOVE -> {
                            floatWindowLayoutUpdateParam.x = (int) ((x + event.getRawX()) - px);
                            floatWindowLayoutUpdateParam.y = (int) ((y + event.getRawY()) - py);
                            windowManager.updateViewLayout(floatView, floatWindowLayoutUpdateParam);
                        }
                    }
                    return false;
                }
            });
        }
        return this;
    }

    public void updatePosition(double x, double y) {
        final WindowManager.LayoutParams floatWindowLayoutUpdateParam = floatWindowLayoutParam;
        floatWindowLayoutUpdateParam.x = (int) x;
        floatWindowLayoutUpdateParam.y = (int) y;
        windowManager.updateViewLayout(floatView, floatWindowLayoutUpdateParam);
    }

    public FloatingAlert setAlpha(float alpha) {
        floatView.setAlpha(alpha);
        return this;
    }

    public void show() {
        if (!PermissionUtil.hasOverlayPermission(_ctx)) {
            return;
        }

        if (isShowing()) {
            return;
        }

        windowManager.addView(floatView, floatWindowLayoutParam);
    }

    public boolean isShowing() {
        if (windowManager == null || floatView == null) {
            return false;
        }

        return floatView.getWindowToken() != null || floatView.isShown();
    }

    public void close() {
        if (!isShowing()) {
            return;
        }

        windowManager.removeView(floatView);
    }

    private void update() {
        windowManager.updateViewLayout(floatView, floatWindowLayoutParam);
    }

    private void setNonTouchable() {
        floatWindowLayoutParam.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        update();
    }

    private void setTouchable() {
        floatWindowLayoutParam.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        update();
    }

    public FloatingAlert bindView(OnViewBinding binding) {
        _onViewBinding = binding;
        return this;
    }

    public FloatingAlert onViewCreated(OnViewCreated onViewCreated) {
        _onViewCreated = onViewCreated;
        return this;
    }

    public FloatingAlert setOnClickListener(OnClickListener onClickListener) {
        _onClickListener = onClickListener;
        return this;
    }

    public FloatingAlert setOnDoubleClickListener(OnClickListener onClickListener) {
        _onDoubleClickListener = onClickListener;
        return this;
    }

    public interface OnViewCreated {
        void onViewCreated(View view, FloatingAlert alert);
    }

    public interface OnViewBinding {
        View onViewCreate(LayoutInflater inflater, FloatingAlert alert);
    }

    public interface OnClickListener {
        void onClick(FloatingAlert alert);
    }
}
