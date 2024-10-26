package com.github.teranes10.androidutils.helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.github.teranes10.androidutils.R;

import java.util.Objects;

public abstract class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private final IBinder binder = new ServiceBinder(this);
    private Context _ctx;
    public boolean _serviceRunning = false;
    private Long _serviceStartedAt;

    protected abstract int getServiceId();

    protected abstract int getServiceType();

    protected abstract void onStartService(Context context);

    protected abstract void onStopService();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_START_FOREGROUND_SERVICE -> _onStartService();
                case ACTION_STOP_FOREGROUND_SERVICE -> _onStopService();
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void _onStartService() {
        Log.i(TAG, "_onStartService: " + getServiceId());
        if (_serviceRunning) {
            return;
        }

        _ctx = this;
        _serviceRunning = true;
        _serviceStartedAt = System.currentTimeMillis();
        startForegroundService();
        onStartService(_ctx);
    }

    private void _onStopService() {
        Log.i(TAG, "_onStopService: " + getServiceId());
        if (!_serviceRunning) {
            return;
        }

        _ctx = null;
        _serviceRunning = false;
        _serviceStartedAt = null;
        onStopService();
        stopForegroundService();
    }

    public Long getServiceElapsedTime() {
        if (_serviceStartedAt == null) {
            return 0L;
        }

        return System.currentTimeMillis() - _serviceStartedAt;
    }

    public Context getContext() {
        return _ctx;
    }

    public static void startService(Context context, Class<? extends ForegroundService> service) {
        Intent service_intent = new Intent(context, service);
        service_intent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
        context.startService(service_intent);
    }

    public static void stopService(Context context, Class<? extends ForegroundService> service) {
        Intent service_intent = new Intent(context, service);
        service_intent.setAction(ForegroundService.ACTION_STOP_FOREGROUND_SERVICE);
        context.startService(service_intent);
    }

    public static void bindService(Context context, Class<? extends ForegroundService> service, ServiceConnection connection) {
        Intent service_intent = new Intent(context, service);
        context.bindService(service_intent, connection, Context.BIND_AUTO_CREATE);
    }

    public static void unbindService(Context context, ServiceConnection connection) {
        context.unbindService(connection);
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(getServiceId(), createNotification(this), getServiceType());
        } else {
            startForeground(getServiceId(), createNotification(this));
        }
    }

    private void stopForegroundService() {
        stopForeground(true);
        stopSelf();
    }

    private static final String CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL";
    private static final String CHANNEL_NAME = "FOREGROUND_SERVICE";

    private static void createNotificationChannel(Context context) {
        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private static Notification createNotification(Context ctx) {
        createNotificationChannel(ctx);
        return new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Service is running...")
                .build();
    }

    public static class ServiceBinder extends Binder {
        private final ForegroundService service;

        public ServiceBinder(ForegroundService service) {
            this.service = service;
        }

        public ForegroundService getService() {
            return service;
        }
    }
}
