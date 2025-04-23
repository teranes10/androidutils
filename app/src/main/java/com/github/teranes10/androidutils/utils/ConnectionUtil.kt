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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionUtil(
    context: Context,
    private var url: String = "https://google.com/generate_204",
    private var internetCheckingInterval: Long = 60000L,
    private var connectionTimeout: Int = 10000,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val connectivityManager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var checkJob: Job? = null

    private val _isAvailable = MutableSharedFlow<Boolean>(1, extraBufferCapacity = 1, BufferOverflow.DROP_OLDEST)
    val isAvailable = _isAvailable.asSharedFlow()

    fun setUrl(url: String) {
        this.url = url
    }

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
                _isAvailable.tryEmit(false)
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
                val isReachable = isReachable()
                delayFactor = if (!isReachable) (delayFactor + 1).coerceAtMost(4) else 1

                val nextDelay = internetCheckingInterval * delayFactor / 4
                delay(nextDelay)
            }
        }
    }

    private fun stopInternetCheck() {
        checkJob?.cancel()
    }

    suspend fun isReachable(): Boolean {
        val isReachable = try {
            NetworkUtil.isReachable(url, connectionTimeout)
        } catch (e: Exception) {
            false
        }

        val lastValue = _isAvailable.replayCache.firstOrNull()
        if (lastValue != isReachable) {
            _isAvailable.tryEmit(isReachable)
        }

        return isReachable
    }

    fun release() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        stopInternetCheck()
    }
}