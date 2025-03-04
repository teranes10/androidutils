package com.github.teranes10.androidutils.utils

import android.content.Context
import okhttp3.Interceptor
import java.lang.ref.WeakReference
import java.util.function.Consumer
import kotlin.concurrent.Volatile

class DefaultConnectionUtil private constructor(ctx: Context, url: String, interval: Int) :
    EventUtil<ConnectionUtil.ConnectionListener?>() {
    private val _connectionUtil = ConnectionUtil(ctx, url, object : ConnectionUtil.ConnectionListener {
        override fun onInternetAvailabilityChanged(isAvailable: Boolean) {
            notifyListeners { it?.onInternetAvailabilityChanged(isAvailable) }
        }
    })

    init {
        _connectionUtil.setInternetCheckingInterval(interval)
    }

    fun addListener(listener: ConnectionUtil.ConnectionListener): Boolean {
        return super.addListener(listener)
    }

    @Synchronized
    fun removeListener(listener: ConnectionUtil.ConnectionListener): Boolean {
        if (super.removeListener(listener)) {
            if (listenerCount == 0) {
                _connectionUtil.release()

                if (instanceWeakRef != null) {
                    instanceWeakRef!!.clear()
                    instanceWeakRef = null
                }
            }

            return true
        }

        return false
    }

    companion object {
        @Volatile
        private var instanceWeakRef: WeakReference<DefaultConnectionUtil>? = null
        private var _url = "https://www.google.com/generate_204"
        private var _interval = 30 * 1000

        @Synchronized
        fun getInstance(ctx: Context): DefaultConnectionUtil {
            var instance = if (instanceWeakRef != null) instanceWeakRef!!.get() else null
            if (instance == null) {
                instance = DefaultConnectionUtil(ctx, _url, _interval)
                instanceWeakRef = WeakReference(instance)
            }

            return instance
        }

        @JvmStatic
        fun setup(url: String, interval: Int) {
            _url = url
            _interval = interval
        }

        @Synchronized
        private fun updateInternetAvailability(isConnected: Boolean) {
            val instance = if (instanceWeakRef != null) instanceWeakRef!!.get() else null
            instance?._connectionUtil?.updateInternetAvailability(isConnected)
        }

        fun interceptor(): Interceptor {
            return Interceptor { chain: Interceptor.Chain ->
                val request = chain.request()
                try {
                    val response = chain.proceed(request)
                    updateInternetAvailability(true)
                    return@Interceptor response
                } catch (e: Exception) {
                    updateInternetAvailability(false)
                    throw e
                }
            }
        }
    }
}
