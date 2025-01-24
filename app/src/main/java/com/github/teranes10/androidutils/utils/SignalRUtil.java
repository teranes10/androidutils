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
import io.reactivex.rxjava3.disposables.Disposable;

public abstract class SignalRUtil {
    private static final String TAG = "SignalR";
    private static final int SERVER_TIMEOUT = 30 * 1000;
    private static final int KEEP_ALIVE_INTERVAL = 20 * 1000;
    private static final int[] CONNECTION_RETRIES = new int[]{0, 10000, 20000, 30000, 60000};
    private static final int RESET_CONNECTION_RETRY_IN = 5 * 60 * 1000;

    private final Context context;
    private final SignalRStatusListener listener;
    private HubConnection hubConnection;
    private long lastReconnectedAt = 0;
    private int currentConnectionTry = 0;
    private boolean isConnecting;
    private CompletableFuture<HubConnection> connectionFuture;
    private Disposable connectionDisposable;

    public enum ConnectionStatus {
        Disconnected,
        Connecting,
        Connected,
        Reconnecting,
        NotConnected
    }

    private final ConnectionUtil.ConnectionListener internetConnectionListener = isConnected -> {
        if (isConnected) {
            updateStatus(ConnectionStatus.NotConnected);
            if (!isConnecting) {
                connect();
            }
        }
    };

    public interface SignalRStatusListener {
        void onSignalRConnectionStatusChanged(ConnectionStatus status);
    }

    public SignalRUtil(Context context, SignalRStatusListener listener) {
        this.context = context;
        this.listener = listener;
    }

    private void updateStatus(ConnectionStatus status) {
        if (listener != null) {
            listener.onSignalRConnectionStatusChanged(status);
        }
    }

    private HubConnection createConnection() {
        if (Utils.isNullOrEmpty(setUrl())) {
            return null;
        }

        HttpHubConnectionBuilder builder = HubConnectionBuilder.create(setUrl());
        if (!Utils.isNullOrEmpty(setToken())) {
            builder.withAccessTokenProvider(Single.defer(() -> Single.just(setToken())));
        }

        HubConnection connection = builder.build();
        connection.setServerTimeout(SERVER_TIMEOUT);
        connection.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);

        setListeners(connection);

        connection.onClosed(e -> {
            Log.e(TAG, "onClosed: " + setTag() + ": " + e.getLocalizedMessage());
            onConnectionClosed();
            updateStatus(ConnectionStatus.Reconnecting);
            reconnect();
        });

        return connection;
    }

    private void reconnect() {
        try {
            long elapsed = SystemClock.elapsedRealtime() - lastReconnectedAt;
            if (elapsed > RESET_CONNECTION_RETRY_IN) {
                currentConnectionTry = 0;
            }

            if (currentConnectionTry < CONNECTION_RETRIES.length) {
                lastReconnectedAt = SystemClock.elapsedRealtime();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isConnecting) {
                        connect();
                    }
                }, CONNECTION_RETRIES[currentConnectionTry]);

                currentConnectionTry++;
            } else {
                onConnectionClosed();
                updateStatus(ConnectionStatus.Disconnected);
            }
        } catch (Exception e) {
            Log.e(TAG, "reconnect: " + e.getLocalizedMessage());
        }
    }

    private void startInternetConnectionObserver() {
        DefaultConnectionUtil instance = DefaultConnectionUtil.getInstance(context);
        if (instance != null) {
            instance.addListener(internetConnectionListener);
        }
    }

    private void stopInternetConnectionObserver() {
        DefaultConnectionUtil instance = DefaultConnectionUtil.getInstance(context);
        if (instance != null) {
            instance.removeListener(internetConnectionListener);
        }
    }

    public String setTag() {
        return getClass().getSimpleName();
    }

    public String setToken() {
        return null;
    }

    protected abstract String setUrl();

    protected abstract void setListeners(HubConnection connection);

    public CompletableFuture<HubConnection> connect() {
        if (isConnected(hubConnection)) {
            return CompletableFuture.completedFuture(hubConnection);
        }

        if (isConnecting && connectionFuture != null) {
            return connectionFuture.thenApply(hubConnection -> {
                if (isConnected(hubConnection)) {
                    Log.i(TAG, "ConnectionId: " + hubConnection.getConnectionId());
                    return hubConnection;
                }
                return null;
            }).exceptionally(e -> {
                Log.e(TAG, "Error while waiting for connection: " + setTag(), e);
                return null;
            });
        }

        if (hubConnection == null) {
            hubConnection = createConnection();
        }

        updateStatus(ConnectionStatus.Connecting);
        isConnecting = true;
        connectionFuture = new CompletableFuture<>();

        dispose();
        connectionDisposable = hubConnection.start().subscribe(
                () -> {
                    connectionFuture.complete(hubConnection);
                    onConnected();
                },
                e -> {
                    Log.e(TAG, "Connection error: " + e.getLocalizedMessage());
                    connectionFuture.complete(null);
                    onConnectionClosed();
                });

        return connectionFuture;
    }

    protected void onConnected() {
        isConnecting = false;
        connectionFuture = null;

        if (isConnected()) {
            Log.i(TAG, "Connected.");
            onConnected();
            updateStatus(ConnectionStatus.Connected);
            stopInternetConnectionObserver();
        } else {
            onConnectionClosed();
        }
    }

    protected void onConnectionClosed() {
        isConnecting = false;
        connectionFuture = null;

        updateStatus(ConnectionStatus.Disconnected);
        startInternetConnectionObserver();
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public boolean isConnected(HubConnection connection) {
        return connection != null && connection.getConnectionState() == HubConnectionState.CONNECTED;
    }

    public boolean isConnected() {
        return isConnected(hubConnection);
    }

    public boolean canConnect() {
        return !isConnecting && !isConnected();
    }

    public CompletableFuture<HubConnection> getConnection(String tag) {
        if (isConnecting && connectionFuture != null) {
            return connectionFuture.thenApply(hubConnection -> {
                if (isConnected(hubConnection)) {
                    Log.i(TAG, "ConnectionId: " + hubConnection.getConnectionId());
                    return hubConnection;
                }
                return null;
            }).exceptionally(e -> {
                Log.e(TAG, "Error waiting for connection: " + tag, e);
                return null;
            });
        }

        return CompletableFuture.completedFuture(isConnected() ? hubConnection : null);
    }

    private Disposable connectionCloseDisposable;

    public CompletableFuture<Void> close() {
        Log.i(TAG, "Closing connection.");
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (isConnected()) {
            dispose();
            connectionCloseDisposable = hubConnection.stop().subscribe(() -> {
                future.complete(null);
                Log.i(TAG, "Connection closed.");
            }, future::completeExceptionally);
        }

        return future;
    }

    public void dispose() {
        if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
            connectionDisposable.dispose();
        }
        if (connectionCloseDisposable != null && !connectionCloseDisposable.isDisposed()) {
            connectionCloseDisposable.dispose();
        }
    }
}
