package com.github.teranes10.androidutils.utils

import android.os.SystemClock
import android.util.Log
import com.github.teranes10.androidutils.extensions.ExceptionExtensions.displayMessage
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

abstract class SignalRUtil(protected val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {

    companion object {
        private const val TAG = "SignalR"
        private const val SERVER_TIMEOUT = 30000L
        private const val KEEP_ALIVE_INTERVAL = 20000L
        private val CONNECTION_RETRIES = listOf(0L, 10000L, 20000L, 30000L, 60000L, 120000L)
        private const val RESET_CONNECTION_RETRY_IN = 5 * 60 * 1000L
    }

    open class ConnectionStatus(val message: String) {
        data object Disconnected : ConnectionStatus("Not connected")
        data object Connecting : ConnectionStatus("Attempting to connect")
        data object Connected : ConnectionStatus("Connected successfully")
        data object Reconnecting : ConnectionStatus("Reconnecting...")
    }

    protected val _events = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val events = _events.asStateFlow()

    private var hubConnection: HubConnection? = null
    private var lastReconnectedAt = 0L
    private var currentConnectionTry = 0
    private val isConnecting = AtomicBoolean(false)
    private val connectionFuture: AtomicReference<Deferred<Unit>?> = AtomicReference(null)
    private val connectionMutex = Mutex()

    val isConnected: Boolean get() = isConnected(hubConnection)

    private fun isConnected(connection: HubConnection?): Boolean {
        return connection?.connectionState == HubConnectionState.CONNECTED
    }

    private fun updateStatus(status: ConnectionStatus) {
        _events.update { status }
    }

    private fun createConnection(): HubConnection? {
        val url = setUrl()
        if (url.isNullOrEmpty()) return null

        val builder = HubConnectionBuilder.create(url)
        setToken()?.let { token -> builder.withAccessTokenProvider(Single.defer { Single.just(token) }) }

        return builder.build().apply {
            serverTimeout = SERVER_TIMEOUT
            keepAliveInterval = KEEP_ALIVE_INTERVAL
            setListeners(this)
            onClosed {
                Log.e(TAG, "onClosed (start reconnection): ${setTag()}: ${it.displayMessage}")
                scope.launch { reconnect() }
            }
        }
    }

    private suspend fun reconnect() {
        val elapsed = SystemClock.elapsedRealtime() - lastReconnectedAt
        if (elapsed > RESET_CONNECTION_RETRY_IN) {
            currentConnectionTry = 0
        }

        if (currentConnectionTry < CONNECTION_RETRIES.size) {
            lastReconnectedAt = SystemClock.elapsedRealtime()

            if (!isConnecting.get()) {
                delay(CONNECTION_RETRIES[currentConnectionTry])
                connect(true, "onReconnect")
            }

            currentConnectionTry++
        } else {
            onConnectionClosed()
        }
    }

    protected abstract fun setUrl(): String?
    protected abstract fun setListeners(connection: HubConnection)
    protected open fun setTag(): String = javaClass.simpleName
    protected open fun setToken(): String? = null

    open fun onConnected() {
        Log.i(TAG, "Connected.")
        updateStatus(ConnectionStatus.Connected)
    }

    open fun onConnectionClosed() {
        Log.e(TAG, "onConnectionClosed: ")
        updateStatus(ConnectionStatus.Disconnected)
    }

    suspend fun connect(reconnect: Boolean = false, tag: String = "") {
        Log.i(TAG, "connect: $tag")

        if (isConnected) {
            Log.i(TAG, "connect: already connected")
            return
        }

        if (!connectionMutex.tryLock(SERVER_TIMEOUT)) {
            Log.e(TAG, "Unable to acquire lock to connect")
            return
        }

        try {
            if (isConnected) {
                Log.i(TAG, "connect: already connected (inside critical section)")
                return
            }

            Log.i(TAG, "connect: connecting")
            isConnecting.set(true)
            updateStatus(if (reconnect) ConnectionStatus.Reconnecting else ConnectionStatus.Connecting)

            if (hubConnection == null) {
                hubConnection = createConnection()
            }

            try {
                hubConnection?.start()?.await()
                onConnected()
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.displayMessage}", e)
                onConnectionClosed()
            } finally {
                isConnecting.set(false)
            }
        } finally {
            connectionMutex.unlock()
        }
    }

    suspend fun getConnection(): HubConnection? {
        if (isConnected) {
            return hubConnection
        }

        if (isConnecting.get()) {
            connectionMutex.withLock {
                return if (isConnected) hubConnection else null
            }
        }

        return null
    }

    suspend fun <T : Any> invoke(
        returnType: Class<T>,
        method: String,
        vararg args: Any
    ): T? = withContext(Dispatchers.IO) {
        try {
            val connection = getConnection()
            if (connection == null) {
                Log.e(TAG, "$method: No connection.")
                return@withContext null
            }

            Log.i(TAG, "$method: ${args.joinToString(", ")}")
            val res = connection.invokeSuspend(returnType, method, *args)
            Log.i(TAG, "$method: ${args.joinToString(", ")} :: $res")
            return@withContext res
        } catch (e: Exception) {
            Log.e(TAG, "$method: ", e)
            return@withContext null
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        if (isConnected) {
            try {
                hubConnection?.stop()?.blockingAwait()
                Log.i(TAG, "Connection closed.")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing connection: ${e.displayMessage}", e)
            }
        }
    }
}