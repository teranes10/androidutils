package com.github.teranes10.androidutils.utils.location

import android.content.Context
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.location.Address as AndroidAddress

class AddressProvider(context: Context) {
    private val geocoder = Geocoder(context, Locale.getDefault())

    companion object {
        private const val TAG = "AddressProvider"
    }

    suspend fun getAddress(latitude: Double, longitude: Double): Address? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getAddressNewAPI(latitude, longitude)
        } else {
            getAddressOldAPI(latitude, longitude)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddressNewAPI(latitude: Double, longitude: Double): Address? {
        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(latitude, longitude, 1, object : GeocodeListener {
                override fun onGeocode(addresses: MutableList<AndroidAddress>) {
                    val address = addresses.firstOrNull()?.toCustomAddress()
                    continuation.resume(address)
                }

                override fun onError(errorMessage: String?) {
                    Log.e(TAG, "Geocoder error: $errorMessage")
                    continuation.resumeWithException(Exception(errorMessage ?: "Unknown error"))
                }
            })
        }
    }

    private suspend fun getAddressOldAPI(latitude: Double, longitude: Double): Address? {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.toCustomAddress()
            } catch (e: Exception) {
                Log.e(TAG, "Geocoder error: ${e.localizedMessage}")
                null
            }
        }
    }

    private fun AndroidAddress.toCustomAddress(): Address {
        return Address(
            addressLine = getAddressLine(maxAddressLineIndex) ?: "",
            streetNo = featureName ?: "",
            streetName = thoroughfare ?: "",
            city = subAdminArea ?: "",
            suburb = locality ?: "",
            state = adminArea ?: "",
            country = countryName ?: "",
            countryCode = countryCode ?: "",
            postCode = postalCode ?: ""
        )
    }

    data class Address(
        val addressLine: String,
        val streetNo: String,
        val streetName: String,
        val city: String,
        val suburb: String,
        val state: String,
        val country: String,
        val countryCode: String,
        val postCode: String
    )
}