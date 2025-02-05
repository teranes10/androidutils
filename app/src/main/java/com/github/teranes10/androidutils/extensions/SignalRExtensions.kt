package com.example.mytaxy.extensions

import com.example.mytaxy.extensions.RxJavaExtensions.await
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

