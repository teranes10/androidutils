package com.github.teranes10.androidutils.extensions

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object CoroutineScopeExtensions {

    fun CoroutineScope.scheduleWithFixedDelay(
        delay: Long,
        initialDelay: Long = 0L,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: suspend (diff: Long) -> Unit
    ): ScheduledExecutorService {
        val executor = Executors.newSingleThreadScheduledExecutor()
        var lastTick = SystemClock.elapsedRealtime()

        executor.scheduleWithFixedDelay({
            val now = SystemClock.elapsedRealtime()
            val diff = now - lastTick
            lastTick = now

            this.launch(dispatcher) {
                try {
                    block(diff)
                } catch (e: Exception) {
                    Log.e("scheduleFixedDelayExecutor", "Error in scheduled block", e)
                }
            }
        }, initialDelay, delay, timeUnit)

        return executor
    }

    val ScheduledExecutorService.isRunning: Boolean get() = !this.isShutdown && !this.isTerminated
}