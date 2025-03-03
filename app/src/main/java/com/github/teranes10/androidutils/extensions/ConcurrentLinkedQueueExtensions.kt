package com.github.teranes10.androidutils.extensions

import java.util.concurrent.ConcurrentLinkedQueue

object ConcurrentLinkedQueueExtensions {

    fun <T> ConcurrentLinkedQueue<T>.pollBatch(batchSize: Int): List<T> {
        val batch = mutableListOf<T>()
        repeat(batchSize) {
            poll()?.let { batch.add(it) } ?: return batch
        }
        return batch
    }
}