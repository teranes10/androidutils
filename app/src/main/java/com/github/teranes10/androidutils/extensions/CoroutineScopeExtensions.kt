package com.github.teranes10.androidutils.extensions

import android.os.SystemClock
import android.util.Log
import com.github.teranes10.androidutils.extensions.ExceptionExtensions.displayMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object CoroutineScopeExtensions {

    fun CoroutineScope.scheduleWithFixedDelay(
        delay: Long,
        initialDelay: Long = 0L,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        executor: ScheduledExecutorService? = null,
        tag: String = "scheduleWithFixedDelay",
        block: suspend (diff: Long) -> Boolean
    ): CancelableSchedule {
        val scope = this
        val createdExecutor = executor ?: Executors.newSingleThreadScheduledExecutor()
        val lastTick = AtomicLong(SystemClock.elapsedRealtime())
        val completion = CompletableDeferred<Unit>()

        var scheduledFuture: ScheduledFuture<*>? = null

        val cancelableSchedule = object : CancelableSchedule {
            override val isNotRunning: Boolean get() = scheduledFuture == null || scheduledFuture?.isCancelled == true || scheduledFuture?.isDone == true
            override val isRunning: Boolean get() = !isNotRunning
            override val lastTick: Long get() = lastTick.get()

            override fun cancel(mayInterruptIfRunning: Boolean) {
                scheduledFuture?.cancel(mayInterruptIfRunning)
                if (executor == null) {
                    createdExecutor.shutdownNow()
                    Log.i(tag, "Executor shutdown")
                }
                if (!completion.isCompleted) completion.complete(Unit)
            }

            override suspend fun await() {
                completion.await()
            }
        }

        scheduledFuture = createdExecutor.scheduleWithFixedDelay({
            val now = SystemClock.elapsedRealtime()
            val diff = now - lastTick.getAndSet(now)

            if (scope.isActive) {
                scope.launch {
                    try {
                        val shouldContinue = block(diff)
                        if (!shouldContinue) {
                            cancelableSchedule.cancel()
                            Log.i(tag, "Schedule stopped by block condition")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error in scheduled block: ${e.displayMessage}", e)
                        cancelableSchedule.cancel()
                        if (!completion.isCompleted) completion.completeExceptionally(e)
                    }
                }
            }
        }, initialDelay, delay, timeUnit)

        this.coroutineContext[Job]?.invokeOnCompletion {
            cancelableSchedule.cancel()
        }

        return cancelableSchedule
    }

    val ScheduledExecutorService.isRunning: Boolean get() = !this.isShutdown && !this.isTerminated
}

interface CancelableSchedule {
    val lastTick: Long
    val isRunning: Boolean
    val isNotRunning: Boolean
    fun cancel(mayInterruptIfRunning: Boolean = false)
    suspend fun await()
}