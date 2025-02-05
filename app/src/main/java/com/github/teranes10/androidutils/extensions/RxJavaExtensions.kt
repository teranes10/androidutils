package com.github.teranes10.androidutils.extensions

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RxJavaExtensions {
    suspend fun <T : Any> Single<T>.await(): T {
        return suspendCancellableCoroutine { cont ->
            val disposable = this.subscribe(
                { result -> cont.resume(result) },
                { error -> cont.resumeWithException(error) }
            )
            cont.invokeOnCancellation { disposable.dispose() }
        }
    }
}