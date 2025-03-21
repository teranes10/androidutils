package com.github.teranes10.androidutils.utils.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import com.github.teranes10.androidutils.models.Outcome

class LocationManagerProvider(context: Context, listener: ILocationListener?) : LocationProvider(context) {
    private val locationListener = LocationListener { location -> listener?.onLocationChanged(location) }

    @SuppressLint("MissingPermission")
    override fun startUpdates(intervalMillis: Long, minIntervalMillis: Long, minDistance: Float): Outcome<*> {
        val result = super.startUpdates(intervalMillis, minIntervalMillis, minDistance)
        if (result.isFailure) {
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
