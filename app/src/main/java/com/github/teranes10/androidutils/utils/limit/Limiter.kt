package com.github.teranes10.androidutils.utils.limit

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.util.concurrent.atomic.AtomicLong

class Limiter<T : Any?>(
    private val minUpdateTime: Long,
    private val onUpdate: (T?) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val lastEmittedTime = AtomicLong(0L)
    private var latestValue: T? = null
    private var job: Job? = null

    fun trigger(value: T? = null) {
        latestValue = value ?: latestValue
        val currentTime = SystemClock.elapsedRealtime()
        val elapsed = currentTime - lastEmittedTime.get()

        if (elapsed >= minUpdateTime) {
            emitValue()
        } else {
            job?.cancel()
            job = scope.launch {
                delay(minUpdateTime - elapsed)
                emitValue()
            }
        }
    }

    private fun emitValue() {
        onUpdate(latestValue)
        lastEmittedTime.set(SystemClock.elapsedRealtime())
    }

    fun cancel() {
        job?.cancel()
        scope.cancel()
    }
}
