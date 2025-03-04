package com.github.teranes10.androidutils.extensions

import android.annotation.SuppressLint
import io.reactivex.rxjava3.core.Completable
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

    @SuppressLint("CheckResult")
    suspend fun Completable.await() = suspendCancellableCoroutine { continuation ->
        this.subscribe(
            { continuation.resume(Unit) },
            { e -> continuation.resumeWithException(e) }
        )
    }
}