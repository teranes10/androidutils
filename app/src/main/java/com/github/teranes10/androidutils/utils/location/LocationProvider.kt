package com.github.teranes10.androidutils.utils.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventCallback
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import com.github.teranes10.androidutils.models.Outcome
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class LocationProvider(private val context: Context) {
    protected val locationManager: LocationManager = context.applicationContext.getSystemService(LocationManager::class.java)
    private val sensorManager: SensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var minIntervalMillis: Long = 0

    private var enableSatellitesInfo: Boolean = false
    private var satelliteListener: ISatelliteListener? = null
    val satellitesInfo: AtomicReference<GnssStatus?> = AtomicReference(null)

    private var enableAccelerometerInfo: Boolean = false
    private var movementListener: IMovementListener? = null
    val movementsInfo: AtomicReference<MovementValues?> = AtomicReference(null)

    private var lastSatellitesUpdate: Long = 0
    private val satelliteStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)
            val now = SystemClock.elapsedRealtime()
            if (minIntervalMillis > 0 && now - lastSatellitesUpdate >= minIntervalMillis) {
                lastSatellitesUpdate = now
                satellitesInfo.set(status)
                satelliteListener?.onStatusChanged(status)
            }
        }
    }

    private var lastSensorUpdate: Long = 0
    private val sensorEventCallback: SensorEventListener = object : SensorEventCallback() {
        override fun onSensorChanged(event: SensorEvent) {
            super.onSensorChanged(event)
            val now = SystemClock.elapsedRealtime()
            if (minIntervalMillis > 0 && now - lastSensorUpdate >= minIntervalMillis) {
                lastSensorUpdate = now
                val values = MovementValues(event.values[0], event.values[1], event.values[2])
                movementsInfo.set(values)
                movementListener?.onMovementChanged(values)
            }
        }
    }

    private var handlerThread: HandlerThread? = null
    protected var handler: Handler? = null

    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    val activeProviders get() = providers.filter { locationManager.isProviderEnabled(it) }

    fun setEnableSatellitesInfo(enable: Boolean, listener: ISatelliteListener? = null) {
        enableSatellitesInfo = enable
        satelliteListener = listener
    }

    fun setEnableAccelerometer(enable: Boolean, listener: IMovementListener? = null) {
        enableAccelerometerInfo = enable
        movementListener = listener
    }

    fun hasPermissions(): Outcome<*> {
        val isSuccess = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (isSuccess) Outcome(true, "Has location permissions.")
        else Outcome.fail(false, "No location permission provided.")
    }

    @SuppressLint("MissingPermission")
    open fun startUpdates(intervalMillis: Long, minIntervalMillis: Long, minDistance: Float): Outcome<*> {
        this.minIntervalMillis = minIntervalMillis

        val permissionResult = hasPermissions()
        if (permissionResult.isFailure) {
            return permissionResult
        }

        if (activeProviders.isEmpty()) {
            return Outcome.fail(false, "No location providers enabled!")
        }

        if (handlerThread == null || handler == null) {
            handlerThread = HandlerThread(TAG).apply { start() }
            handler = Handler(handlerThread!!.looper)
        }

        if (enableSatellitesInfo && activeProviders.contains(LocationManager.GPS_PROVIDER)) {
            locationManager.registerGnssStatusCallback(satelliteStatusCallback, handler)
        }

        if (enableAccelerometerInfo) {
            sensorManager.registerListener(
                sensorEventCallback, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, handler
            )
        }

        return Outcome.ok(true, "Location updates started with ${activeProviders.joinToString(",")}")
    }

    open fun stopUpdates() {
        if (enableSatellitesInfo)
            locationManager.unregisterGnssStatusCallback(satelliteStatusCallback)

        if (enableAccelerometerInfo)
            sensorManager.unregisterListener(sensorEventCallback)

        handlerThread?.apply {
            quitSafely()
            join()
        }
        handlerThread = null
        handler = null
    }

    open fun updateInterval(intervalMillis: Long, minIntervalMillis: Long, minDistance: Float) {
        stopUpdates()
        startUpdates(intervalMillis, minIntervalMillis, minDistance)
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Outcome<Location> {
        val permissionResult = hasPermissions()
        if (permissionResult.isFailure) {
            return Outcome.fail(permissionResult.message)
        }

        return suspendCoroutine { continuation ->
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location == null || (location.latitude == 0.0 && location.longitude == 0.0)) {
                continuation.resume(Outcome.fail(null, "No location found."))
            } else {
                continuation.resume(Outcome.ok(location, "Success"))
            }
        }
    }

    interface ILocationListener {
        fun onLocationChanged(location: Location)
    }

    interface ISatelliteListener {
        fun onStatusChanged(status: GnssStatus)
    }

    interface IMovementListener {
        fun onMovementChanged(values: MovementValues)
    }

    companion object {
        private const val TAG = "LocationProvider"
    }
}