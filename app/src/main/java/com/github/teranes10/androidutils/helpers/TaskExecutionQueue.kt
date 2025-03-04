package com.github.teranes10.androidutils.helpers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class TaskExecutionQueue(
    private val context: Context,
    private val canExecuteTask: (() -> Boolean)? = null
) {
    companion object {
        private const val TAG = "TaskExecutionQueueService"
    }

    private val tasks: Queue<Task> = ArrayDeque()
    private val mutex = Mutex()
    private val isExecuting = AtomicBoolean(false)

    fun add(task: Task?) {
        if (task == null) return

        CoroutineScope(Dispatchers.IO).launch {
            if (execute(task)) return@launch

            mutex.withLock {
                tasks.add(task)
                Log.i(TAG, "addNewTask: added.")
            }
        }
    }

    fun execute() {
        if (isExecuting.getAndSet(true)) return

        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                Log.i(TAG, "executeTasks: ${tasks.size}")

                while (tasks.isNotEmpty()) {
                    val task = tasks.remove()
                    execute(task)
                }
            }
            isExecuting.set(false)
        }
    }

    private fun execute(task: Task): Boolean {
        val canExecute = canExecuteTask?.invoke() ?: true

        return if (canExecute) {
            try {
                task.execute(context)
                Log.i(TAG, "addNewTask: executed.")
                true
            } catch (e: Exception) {
                Log.e(TAG, "addNewTask: ", e)
                false
            }
        } else {
            false
        }
    }

    fun interface Task {
        fun execute(context: Context)
    }
}
