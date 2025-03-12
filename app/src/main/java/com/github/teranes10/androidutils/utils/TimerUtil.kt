package com.github.teranes10.androidutils.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class TimerUtil(
    private var interval: Long,
    private val tag: String = "TimerUtil",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private var job: Job? = null

    abstract suspend fun execute()

    fun start(immediate: Boolean = false) {
        if (job?.isActive == true) return

        job = scope.launch {
            if(immediate) {
                executeSafely()
            }

            while (isActive) {
                delay(interval)
                executeSafely()
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
        if (interval != newInterval) {
            interval = newInterval
            restart()
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