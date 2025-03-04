package com.github.teranes10.androidutils.utils

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.github.teranes10.androidutils.extensions.RxJavaExtensions.await
import com.github.teranes10.androidutils.extensions.SignalRExtensions.invokeSuspend
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

abstract class SignalRUtil(
    private val context: Context,
    private val listener: SignalRStatusListener? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    companion object {
        private const val TAG = "SignalR"
        private const val SERVER_TIMEOUT = 30_000L
        private const val KEEP_ALIVE_INTERVAL = 20_000L
        private val CONNECTION_RETRIES = listOf(0L, 10_000L, 20_000L, 30_000L, 60_000L)
        private const val RESET_CONNECTION_RETRY_IN = 5 * 60 * 1000L
    }

    private var hubConnection: HubConnection? = null
    private var lastReconnectedAt = 0L
    private var currentConnectionTry = 0
    private val isConnecting = AtomicBoolean(false)

    private val connectionMutex = Mutex()
    private var connectionFuture: Deferred<HubConnection?>? = null

    enum class ConnectionStatus {
        Disconnected, Connecting, Connected, Reconnecting, NotConnected
    }

    interface SignalRStatusListener {
        fun onSignalRConnectionStatusChanged(status: ConnectionStatus?)
    }

    private val internetConnectionListener = object : ConnectionUtil.ConnectionListener {
        override fun onInternetAvailabilityChanged(isAvailable: Boolean) {
            if (isAvailable) {
                if (!isConnecting.get()) {
                    scope.launch { connect() }
                }
            } else {
                updateStatus(ConnectionStatus.NotConnected)
            }
        }
    }

    private fun updateStatus(status: ConnectionStatus) {
        listener?.onSignalRConnectionStatusChanged(status)
    }

    private fun createConnection(): HubConnection? {
        val url = setUrl()
        if (url.isNullOrEmpty()) return null

        val builder = HubConnectionBuilder.create(url)
        setToken()?.let { token ->
            builder.withAccessTokenProvider(Single.defer { Single.just(token) })
        }

        return builder.build().apply {
            serverTimeout = SERVER_TIMEOUT
            keepAliveInterval = KEEP_ALIVE_INTERVAL
            setListeners(this)
            onClosed {
                Log.e(TAG, "onClosed: ${setTag()}: ${it.localizedMessage}")
                onConnectionClosed()
                updateStatus(ConnectionStatus.Reconnecting)
                scope.launch { reconnect() }
            }
        }
    }

    private suspend fun reconnect() {
        connectionMutex.withLock {
            val elapsed = SystemClock.elapsedRealtime() - lastReconnectedAt
            if (elapsed > RESET_CONNECTION_RETRY_IN) {
                currentConnectionTry = 0
            }

            if (currentConnectionTry < CONNECTION_RETRIES.size) {
                lastReconnectedAt = SystemClock.elapsedRealtime()
                delay(CONNECTION_RETRIES[currentConnectionTry])

                if (!isConnecting.get()) {
                    connect()
                }

                currentConnectionTry++
            } else {
                onConnectionClosed()
                updateStatus(ConnectionStatus.Disconnected)
            }
        }
    }

    private fun startInternetConnectionObserver() {
        DefaultConnectionUtil.getInstance(context).addListener(internetConnectionListener)
    }

    private fun stopInternetConnectionObserver() {
        DefaultConnectionUtil.getInstance(context).removeListener(internetConnectionListener)
    }

    protected open fun setTag(): String = javaClass.simpleName
    protected open fun setToken(): String? = null
    protected abstract fun setUrl(): String?
    protected abstract fun setListeners(connection: HubConnection)

    suspend fun connect(): HubConnection? {
        connectionFuture?.let { return it.await() }

        if (isConnected()) {
            return hubConnection
        }

        updateStatus(ConnectionStatus.Connecting)
        isConnecting.set(true)

        connectionFuture = coroutineScope {
            async(Dispatchers.IO) {
                try {
                    if (hubConnection == null) {
                        hubConnection = createConnection()
                    }

                    hubConnection?.start()?.await()
                    onConnected()
                    hubConnection
                } catch (e: Exception) {
                    Log.e(TAG, "Connection error: ${e.localizedMessage}", e)
                    onConnectionClosed()
                    null
                } finally {
                    isConnecting.set(false)
                    connectionFuture = null
                }
            }
        }

        return connectionFuture?.await()
    }

    protected fun onConnected() {
        Log.i(TAG, "Connected.")
        updateStatus(ConnectionStatus.Connected)
        stopInternetConnectionObserver()
    }

    private fun onConnectionClosed() {
        Log.e(TAG, "onConnectionClosed: ")
        updateStatus(ConnectionStatus.Disconnected)
        startInternetConnectionObserver()
    }

    fun isConnected(): Boolean = isConnected(hubConnection)

    private fun isConnected(connection: HubConnection?): Boolean {
        return connection?.connectionState == HubConnectionState.CONNECTED
    }

    suspend fun getConnection(): HubConnection? {
        connectionFuture?.let { return it.await() }
        return if (isConnected()) hubConnection else null
    }

    suspend fun <T : Any> invoke(
        returnType: Class<T>,
        method: String,
        vararg args: Any
    ): T? {
        try {
            val connection = getConnection()
            if (connection == null) {
                Log.e(TAG, "$method: No connection.")
                return null
            }

            Log.i(TAG, "$method: ${args.joinToString(", ")}")
            val res = connection.invokeSuspend(returnType, method, *args)
            Log.i(TAG, "$method: ${args.joinToString(", ")} :: $res")
            return res
        } catch (e: Exception) {
            Log.e(TAG, "$method: ", e)
            return null
        }
    }

    suspend fun close() {
        withContext(Dispatchers.IO) {
            if (isConnected()) {
                try {
                    hubConnection?.stop()?.blockingAwait()
                    Log.i(TAG, "Connection closed.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing connection: ${e.localizedMessage}", e)
                }
            }
        }
    }
}
