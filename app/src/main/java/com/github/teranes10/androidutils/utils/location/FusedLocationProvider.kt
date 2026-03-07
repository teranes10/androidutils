package com.github.teranes10.androidutils.utils.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.github.teranes10.androidutils.models.Outcome
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class FusedLocationProvider(
    context: Context,
    listener: ILocationListener?,
    sensorType: Int = Sensor.TYPE_LINEAR_ACCELERATION,
    sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME,
    lowPassFilterAlpha: Float = 0.2f,
    magnitudeHistorySize: Int = 50
) : LocationProvider(
    context,
    sensorType = sensorType,
    sensorDelay = sensorDelay,
    lowPassFilterAlpha = lowPassFilterAlpha,
    magnitudeHistorySize = magnitudeHistorySize
) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { listener?.onLocationChanged(it) }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startUpdates(intervalMillis: Long, minIntervalMillis: Long, minDistance: Float): Outcome<*> {
        val result = super.startUpdates(intervalMillis, minIntervalMillis, minDistance)
        if (result.failure) {
            return result
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(minIntervalMillis)
            .setMinUpdateDistanceMeters(minDistance)
            .setWaitForAccurateLocation(true)
            .build()

        fusedClient.requestLocationUpdates(locationRequest, locationCallback, handler!!.looper)
        return result
    }

    override fun stopUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
        super.stopUpdates()
    }
}
