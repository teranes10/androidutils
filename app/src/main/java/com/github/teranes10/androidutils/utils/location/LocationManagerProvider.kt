package com.github.teranes10.androidutils.utils.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationListener
import com.github.teranes10.androidutils.models.Outcome

class LocationManagerProvider(
    context: Context, listener: ILocationListener?, sensorType: Int = Sensor.TYPE_LINEAR_ACCELERATION,
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
    private val locationListener = LocationListener { location -> listener?.onLocationChanged(location) }

    @SuppressLint("MissingPermission")
    override fun startUpdates(intervalMillis: Long, minIntervalMillis: Long, minDistance: Float): Outcome<*> {
        val result = super.startUpdates(intervalMillis, minIntervalMillis, minDistance)
        if (result.failure) {
            return result
        }

        for (provider in activeProviders) {
            locationManager.requestLocationUpdates(provider, minIntervalMillis, minDistance, locationListener, handler!!.looper)
        }

        return result
    }

    override fun stopUpdates() {
        locationManager.removeUpdates(locationListener)
        super.stopUpdates()
    }
}
