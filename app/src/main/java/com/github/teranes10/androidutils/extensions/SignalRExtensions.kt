package com.github.teranes10.androidutils.extensions

import com.github.teranes10.androidutils.extensions.RxJavaExtensions.await
import com.microsoft.signalr.HubConnection

object SignalRExtensions {
    suspend fun <T : Any> HubConnection.invokeSuspend(
        returnType: Class<T>,
        method: String,
        vararg args: Any
    ): T? {
        return this.invoke(returnType, method, *args).await()
    }
}

