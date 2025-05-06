package com.github.teranes10.androidutils.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat

abstract class ForegroundService(
    private val serviceId: Int,
    private val serviceType: Int,
    private val startType: Int = START_STICKY
) : Service() {

    companion object {
        private const val TAG = "ForegroundService"
        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
        private const val CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL"
        private const val CHANNEL_NAME = "FOREGROUND_SERVICE"

        fun startService(context: Context, service: Class<out ForegroundService>, extras: Bundle? = null) {
            val intent = Intent(context, service).apply {
                action = ACTION_START_FOREGROUND_SERVICE
                extras?.let { putExtras(it) }
            }

            context.startService(intent)

            if (!isServiceDeclared(context, service)) {
                Log.e(TAG, "startService: service must be declared in the AndroidManifest.xml!")
            }
        }

        fun stopService(context: Context, service: Class<out ForegroundService>, extras: Bundle? = null) {
            val intent = Intent(context, service).apply {
                action = ACTION_STOP_FOREGROUND_SERVICE
                extras?.let { putExtras(it) }
            }

            context.startService(intent)
        }

        fun bindService(context: Context, service: Class<out ForegroundService>, connection: ServiceConnection) {
            val intent = Intent(context, service)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun unbindService(context: Context, connection: ServiceConnection) {
            context.unbindService(connection)
        }

        private fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        private fun isServiceDeclared(context: Context, serviceClass: Class<out Service>): Boolean {
            val packageManager = context.packageManager
            val componentName = ComponentName(context, serviceClass)
            try {
                packageManager.getServiceInfo(componentName, PackageManager.GET_META_DATA)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "isServiceDeclared: ", e)
                return false
            }
        }
    }

    private val binder = ServiceBinder(this)
    var context: Context? = null; private set
    var running: Boolean = false; private set
    var startedAt: Long? = null; private set
    val elapsedTime: Long get() = startedAt?.let { SystemClock.elapsedRealtime() - it } ?: 0L

    protected abstract fun onStartService(context: Context)
    protected abstract fun onStopService(context: Context)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "service onStartCommand: ${intent?.action}")

        if (intent == null) {
            startServiceInternal()
            return startType
        }

        onCommand(intent, flags, startId)?.let { return it }

        when (intent.action) {
            ACTION_START_FOREGROUND_SERVICE -> startServiceInternal()
            ACTION_STOP_FOREGROUND_SERVICE -> stopServiceInternal()
        }

        return startType
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun startServiceInternal() {
        Log.i(TAG, "startServiceInternal: $serviceId")
        if (running) return

        context = this
        running = true
        startedAt = SystemClock.elapsedRealtime()
        startForegroundCompat()
        onStartService(context!!)
    }

    private fun stopServiceInternal() {
        Log.i(TAG, "stopServiceInternal: $serviceId")
        if (!running) return

        onStopService(context!!)
        stopForegroundCompat()

        context = null
        running = false
        startedAt = null
    }

    private fun startForegroundCompat() {
        val notification = createNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(serviceId, notification, serviceType)
        } else {
            startForeground(serviceId, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotification(context: Context): Notification {
        createNotificationChannel(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, serviceId, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentText("Service is running...")
            .setContentIntent(pendingIntent)

        return onBuildNotification(builder).build()
    }

    open fun onBuildNotification(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        return builder
    }

    open fun onCommand(intent: Intent, flags: Int, startId: Int): Int? {
        return null
    }

    class ServiceBinder(val service: ForegroundService) : Binder()
}