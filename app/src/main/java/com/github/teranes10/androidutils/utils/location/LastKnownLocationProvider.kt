package com.github.teranes10.androidutils.utils.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object LastKnownLocationProvider {

    suspend fun getLastKnownLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }

        val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null

        suspendCoroutine { continuation ->
            val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            if (location == null || (location.latitude == 0.0 && location.longitude == 0.0)) {
                continuation.resume(null)
            } else {
                continuation.resume(location)
            }
        }
    }
}