package com.example.mytaxy.extensions

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CompletableFutureExtensions {
    suspend fun <T> CompletableFuture<T>.await(): T {
        return suspendCancellableCoroutine { cont ->
            this.whenComplete { result, throwable ->
                if (throwable != null) cont.resumeWithException(throwable)
                else cont.resume(result)
            }
        }
    }
}