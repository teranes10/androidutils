package com.github.teranes10.androidutils.extensions

import android.os.SystemClock
import android.util.Log
import com.github.teranes10.androidutils.extensions.ExceptionExtensions.displayMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object CoroutineScopeExtensions {

    fun CoroutineScope.scheduleWithFixedDelay(
        delay: Long,
        initialDelay: Long = 0L,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        executor: ScheduledExecutorService? = null,
        tag: String = "scheduleWithFixedDelay",
        block: suspend (diff: Long) -> Unit
    ): CancelableSchedule {
        val createdExecutor = executor ?: Executors.newSingleThreadScheduledExecutor()
        val lastTick = AtomicLong(SystemClock.elapsedRealtime())

        val future = createdExecutor.scheduleWithFixedDelay({
            val now = SystemClock.elapsedRealtime()
            val diff = now - lastTick.getAndSet(now)

            this.launch {
                try {
                    block(diff)
                } catch (e: Exception) {
                    Log.e(tag, "Error in scheduled block: ${e.displayMessage}", e)
                }
            }
        }, initialDelay, delay, timeUnit)

        this.coroutineContext[Job]?.invokeOnCompletion {
            future.cancel(false)
            if (executor == null) {
                createdExecutor.shutdownNow()
                Log.i(tag, "shutdown")
            }
        }

        return object : CancelableSchedule {
            override val lastTick: Long get() = lastTick.get()
            override val isRunning: Boolean get() = !future.isCancelled && !future.isDone

            override fun cancel(mayInterruptIfRunning: Boolean) {
                future.cancel(mayInterruptIfRunning)
                if (executor == null) {
                    createdExecutor.shutdownNow()
                    Log.i(tag, "shutdown")
                }
            }
        }
    }

    val ScheduledExecutorService.isRunning: Boolean get() = !this.isShutdown && !this.isTerminated
}

interface CancelableSchedule {
    val lastTick: Long
    val isRunning: Boolean
    fun cancel(mayInterruptIfRunning: Boolean = false)
}