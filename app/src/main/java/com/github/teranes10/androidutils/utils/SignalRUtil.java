package com.github.teranes10.androidutils.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.microsoft.signalr.HttpHubConnectionBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.Single;

public abstract class SignalRUtil {
    private static final String TAG = "SignalR";
    private static final int SERVER_TIMEOUT = 30 * 1000;
    private static final int KEEP_ALIVE_INTERVAL = 20 * 1000;
    private static final int[] CONNECTION_RETRIES = new int[]{0, 10000, 20000, 30000, 60000};
    private static final int RESET_CONNECTION_RETRY_IN = 5 * 60 * 1000;

    private final Context _ctx;
    private final SignalRStatusListener _listener;
    private HubConnection _hubConnection;
    private long _lastReconnectedAt = 0;
    private int _currentConnectionTry = 0;
    private boolean _isConnecting;
    private CompletableFuture<HubConnection> _connectionFuture;

    private final ConnectionUtil.ConnectionListener _internetConnectionListener = (isConnected) -> {
        if (isConnected) {
            updateStatus(ConnectionStatus.NotConnected);
            if (!_isConnecting) {
                connect();
            }
        }
    };

    public interface SignalRStatusListener {
        void onSignalRConnectionStatusChanged(ConnectionStatus status);
    }

    public SignalRUtil(Context context, SignalRStatusListener listener) {
        this._ctx = context;
        this._listener = listener;
    }

    private void updateStatus(ConnectionStatus status) {
        if (_listener != null) {
            _listener.onSignalRConnectionStatusChanged(status);
        }
    }

    private HubConnection connection() {
        if (Utils.isNullOrEmpty(setUrl())) {
            return null;
        }

        HttpHubConnectionBuilder builder = HubConnectionBuilder.create(setUrl());
        if (!Utils.isNullOrEmpty(setToken())) {
            builder.withAccessTokenProvider(Single.defer(() -> Single.just(setToken())));
        }

        HubConnection hubConnection = builder.build();
        hubConnection.setServerTimeout(SERVER_TIMEOUT);
        hubConnection.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);

        setListeners(hubConnection);

        hubConnection.onClosed(e -> {
            Log.e(TAG, "onClosed: " + setTag() + ": " + e.getLocalizedMessage());
            onConnectionClosed();
            updateStatus(ConnectionStatus.Reconnecting);
            reconnect();
        });

        return hubConnection;
    }

    private void reconnect() {
        try {
            if ((SystemClock.elapsedRealtime() - _lastReconnectedAt) > RESET_CONNECTION_RETRY_IN) {
                _currentConnectionTry = 0;
            }
            if (_currentConnectionTry < CONNECTION_RETRIES.length) {
                _lastReconnectedAt = SystemClock.elapsedRealtime();
                new Handler(Looper.getMainLooper())
                        .postDelayed(() -> {
                            if (!_isConnecting) {
                                connect();
                            }
                        }, CONNECTION_RETRIES[_currentConnectionTry]);
                _currentConnectionTry++;
            } else {
                onConnectionClosed();
                updateStatus(ConnectionStatus.Disconnected);
            }
        } catch (Exception e) {
            Log.e(TAG, "reconnect: " + e.getLocalizedMessage());
        }
    }

    private void startObserveInternetConnection() {
        DefaultConnectionUtil instance = DefaultConnectionUtil.getInstance(_ctx);
        if (instance != null) {
            instance.addListener(_internetConnectionListener);
        }
    }

    private void stopObserveInternetConnection() {
        DefaultConnectionUtil instance = DefaultConnectionUtil.getInstance(_ctx);
        if (instance != null) {
            instance.removeListener(_internetConnectionListener);
        }
    }

    private String setTag() {
        return getClass().getSimpleName();
    }

    private String setToken() {
        return null;
    }

    protected abstract String setUrl();

    protected abstract void onConnected();

    protected abstract void onConnectionClosed();

    protected abstract void setListeners(HubConnection connection);

    public CompletableFuture<HubConnection> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                updateStatus(ConnectionStatus.Connecting);

                if (_hubConnection == null) {
                    _hubConnection = connection();
                }

                if (!_isConnecting && (_hubConnection != null && !isConnected())) {

                    _isConnecting = true;
                    _connectionFuture = _connect();
                    _connectionFuture.get();
                    _isConnecting = false;
                    _connectionFuture = null;

                    if (!isConnected()) {
                        throw new Exception("Not Connected");
                    }

                    onConnected();
                    updateStatus(ConnectionStatus.Connected);
                    Log.i(TAG, "connection: Connected.");
                    stopObserveInternetConnection();
                }
                return _hubConnection;
            } catch (Exception e) {
                Log.e(TAG, "connection: " + setTag() + ": " + e.getLocalizedMessage());
                onConnectionClosed();
                updateStatus(ConnectionStatus.Disconnected);
                _isConnecting = false;
                _connectionFuture = null;
                startObserveInternetConnection();
                return null;
            }
        });
    }

    private CompletableFuture<HubConnection> _connect() {
        return CompletableFuture.supplyAsync(() -> {
            _hubConnection.start().blockingAwait();
            return _hubConnection;
        }).exceptionally((err) -> null);
    }

    public CompletableFuture<Void> close() {
        Log.i(TAG, "close: ");
        return CompletableFuture.runAsync(() -> {
            try {
                if (isConnected()) {
                    _hubConnection.stop().blockingAwait();
                    Log.i(TAG, "closed: ");
                }
            } catch (Exception e) {
                Log.e(TAG, "close: " + setTag() + ": ", e);
            }
        });
    }

    public boolean isConnecting() {
        return _isConnecting;
    }

    public boolean isConnected() {
        return _hubConnection != null &&
                _hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }

    public CompletableFuture<HubConnection> getConnection(String Tag) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (_isConnecting && _connectionFuture != null) {
                    _connectionFuture.join();
                }

                if (isConnected()) {
                    Log.i(TAG, "getConnection: ConnectionId:" + _hubConnection.getConnectionId());
                    return _hubConnection;
                }
            } catch (Exception e) {
                Log.e(TAG, "getConnection: " + setTag() + ": " + Tag + ": ", e);
            }

            return null;
        });
    }

    public enum ConnectionStatus {
        Disconnected,
        Connecting,
        Connected,
        Reconnecting,
        NotConnected
    }
}
