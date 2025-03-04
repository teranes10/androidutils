package com.github.teranes10.androidutils.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionUtil(
    context: Context,
    private val url: String,
    private val listener: ConnectionListener,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val connectivityManager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val isAvailable = AtomicBoolean(false)
    private var internetCheckingInterval = 60_000L // 1 min
    private var connectionTimeout = 10_000 // 10 sec
    private var checkJob: Job? = null


    fun setInternetCheckingInterval(interval: Int) {
        internetCheckingInterval = interval.toLong()
    }

    fun setRequestConnectionTimeout(timeout: Int) {
        connectionTimeout = timeout
    }

    init {
        initializeNetworkCallback()
    }

    private fun initializeNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                startInternetCheck()
            }

            override fun onLost(network: Network) {
                updateInternetAvailability(false)
                stopInternetCheck()
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback!!)
    }

    private fun startInternetCheck() {
        checkJob?.cancel()
        checkJob = scope.launch {
            var delayFactor = 1L

            while (isActive) {
                val isReachable = try {
                    NetworkUtil.isReachable(url, connectionTimeout)
                } catch (e: Exception) {
                    false
                }

                updateInternetAvailability(isReachable)

                delayFactor = if (!isReachable) (delayFactor + 1).coerceAtMost(4) else 1

                val nextDelay = internetCheckingInterval * delayFactor / 4
                delay(nextDelay)
            }
        }
    }

    private fun stopInternetCheck() {
        checkJob?.cancel()
    }

    fun updateInternetAvailability(available: Boolean) {
        if (isAvailable.getAndSet(available) != available) {
            listener.onInternetAvailabilityChanged(available)
        }
    }

    fun release() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        stopInternetCheck()
    }

    interface ConnectionListener {
        fun onInternetAvailabilityChanged(isAvailable: Boolean)
    }

    companion object {
        private const val TAG = "ConnectionUtil"
    }
}
