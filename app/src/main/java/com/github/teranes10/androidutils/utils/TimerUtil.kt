package com.github.teranes10.androidutils.utils

import android.os.SystemClock
import android.util.Log
import com.github.teranes10.androidutils.extensions.CancelableSchedule
import com.github.teranes10.androidutils.extensions.CoroutineScopeExtensions
import com.github.teranes10.androidutils.extensions.CoroutineScopeExtensions.scheduleWithFixedDelay
import com.github.teranes10.androidutils.extensions.ExceptionExtensions.displayMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

abstract class TimerUtil(
    private var interval: Long,
    private val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    private val tag: String = "TimerUtil",
    private val executor: ScheduledExecutorService? = null,
    private val scope: CoroutineScope? = null
) {
    private val createdScope = scope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cancelableSchedule: CancelableSchedule? = null

    val isRunning: Boolean get() = cancelableSchedule?.isRunning == true

    abstract suspend fun execute()

    fun start(initialDelay: Long = 0) {
        if (interval <= 0 || cancelableSchedule?.isRunning == true) return

        cancelableSchedule = createdScope.scheduleWithFixedDelay(
            delay = interval,
            initialDelay = initialDelay,
            timeUnit = timeUnit,
            executor = executor
        ) {
            execute()
        }
    }

    fun stop() {
        cancelableSchedule?.cancel()
        cancelableSchedule = null
    }

    fun restart(initialDelay: Long = 0) {
        stop()
        start(initialDelay)
    }

    fun updateInterval(newInterval: Long) {
        if (newInterval <= 0 || interval == newInterval) return
        interval = newInterval

        val elapsed = cancelableSchedule?.let { SystemClock.elapsedRealtime() - it.lastTick } ?: 0
        val remaining = (newInterval - elapsed).coerceAtLeast(0)

        restart(remaining)
    }

    fun destroy() {
        stop()

        if (scope == null) {
            createdScope.cancel()
        }
    }
}