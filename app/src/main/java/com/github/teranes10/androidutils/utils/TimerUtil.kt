package com.github.teranes10.androidutils.utils

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

abstract class TimerUtil(
    private var interval: Long,
    private val tag: String = "TimerUtil",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private var job: Job? = null
    private val lastExecutionTime = AtomicLong(0L)

    abstract suspend fun execute()

    fun start(immediate: Boolean = false) {
        if (job?.isActive == true || interval <= 0) return

        job = scope.launch {
            if (immediate) {
                executeSafely()
                lastExecutionTime.set(SystemClock.elapsedRealtime())
            }

            while (isActive) {
                delay(interval)
                executeSafely()
                lastExecutionTime.set(SystemClock.elapsedRealtime())
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun restart(immediate: Boolean = false) {
        stop()
        start(immediate)
    }

    fun updateInterval(newInterval: Long) {
        if (newInterval <= 0 || interval == newInterval) return

        interval = newInterval

        val elapsed = SystemClock.elapsedRealtime() - lastExecutionTime.get()
        val remaining = (newInterval - elapsed).coerceAtLeast(0)

        if (job?.isActive == true) {
            job?.cancel()
            job = scope.launch {
                delay(remaining)
                start(immediate = true)
            }
        }
    }

    private suspend fun executeSafely() {
        try {
            execute()
        } catch (e: Exception) {
            println("TimerUtil:$tag - Error: ${e.message}")
        }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}