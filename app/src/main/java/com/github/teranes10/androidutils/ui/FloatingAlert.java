package com.github.teranes10.androidutils.ui;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
    private float _alpha = 1;
    private Integer _layout;
    private View floatView;
    private WindowManager.LayoutParams floatWindowLayoutParam;
    private WindowManager windowManager;
    private OnViewCreated _onViewCreated;
    private OnViewBinding _onViewBinding;
    private OnClickListener _onClickListener;
    private OnClickListener _onLongClickListener;
    private OnClickListener _onDoubleClickListener;
    private final Context _ctx;
    private static final String TAG = "FloatingAlert";

    public FloatingAlert(Context context, Integer layout) {
        _ctx = context;
        _layout = layout;
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

    public FloatingAlert setAlpha(float alpha) {
        _alpha = alpha;
        return this;
    }

    private boolean buildView() {
        try {
            if (!PermissionUtil.hasOverlayPermission(_ctx)) {
                Log.e(TAG, "buildView: no overlay permission");
                return false;
            }

            windowManager = (WindowManager) _ctx.getApplicationContext().getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "buildView: window manager is null");
                return false;
            }

            LayoutInflater inflater = (LayoutInflater) _ctx.getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            if (inflater == null) {
                Log.e(TAG, "buildView: layout inflater is null");
                return false;
            }

            if (_layout != null) {
                floatView = inflater.inflate(_layout, null);
                return true;
            }

            if (_onViewBinding != null) {
                floatView = _onViewBinding.onViewCreate(inflater, this);
                return true;
            }

            Log.e(TAG, "buildView: layout is null");
        } catch (Exception e) {
            Log.e(TAG, "buildView: ", e);
        }

        return false;
    }

    private void setupParameters() {
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
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
            );
        }

        floatWindowLayoutParam.gravity = _gravity;
        floatWindowLayoutParam.x = _x;
        floatWindowLayoutParam.y = _y;
    }

    private void setupTouchListener() {
        if (windowManager == null || floatView == null || floatWindowLayoutParam == null) {
            Log.e(TAG, "setupTouchListener: invalid arguments");
            return;
        }

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

                @Override
                public void onLongPress(@NonNull MotionEvent e) {
                    if (_onLongClickListener != null) {
                        _onLongClickListener.onClick(FloatingAlert.this);
                    }
                    super.onLongPress(e);
                }
            });

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);

                if (_draggable) {
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
                }

                return false;
            }
        });
    }

    public FloatingAlert build() {
        if (!buildView() || floatView == null) {
            return this;
        }

        floatView.setAlpha(_alpha);

        if (_onViewCreated != null) {
            _onViewCreated.onViewCreated(floatView, this);
        }

        setupParameters();
        setupTouchListener();

        return this;
    }

    public void updatePosition(double x, double y) {
        if (windowManager == null || floatView == null || floatWindowLayoutParam == null) {
            Log.e(TAG, "updatePosition: invalid arguments");
            return;
        }

        final WindowManager.LayoutParams floatWindowLayoutUpdateParam = floatWindowLayoutParam;
        floatWindowLayoutUpdateParam.x = (int) x;
        floatWindowLayoutUpdateParam.y = (int) y;
        windowManager.updateViewLayout(floatView, floatWindowLayoutUpdateParam);
    }

    public View getView() {
        return floatView;
    }

    public boolean show() {
        if (windowManager == null || floatView == null || floatWindowLayoutParam == null) {
            Log.e(TAG, "show: invalid arguments");
            return false;
        }

        if (isShowing()) {
            return false;
        }

        windowManager.addView(floatView, floatWindowLayoutParam);
        return true;
    }

    public void show(int timer) {
        if (show()) {
            new Handler(Looper.getMainLooper())
                    .postDelayed(this::close, timer);
        }
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

    public interface OnViewCreated {
        void onViewCreated(View view, FloatingAlert alert);
    }

    public interface OnViewBinding {
        View onViewCreate(LayoutInflater inflater, FloatingAlert alert);
    }

    public interface OnClickListener {
        void onClick(FloatingAlert alert);
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

    public FloatingAlert setOnLongClickListener(OnClickListener onClickListener) {
        _onLongClickListener = onClickListener;
        return this;
    }

    public FloatingAlert setOnDoubleClickListener(OnClickListener onClickListener) {
        _onDoubleClickListener = onClickListener;
        return this;
    }
}
