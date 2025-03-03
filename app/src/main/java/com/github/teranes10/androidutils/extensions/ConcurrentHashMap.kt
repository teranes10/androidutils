package com.github.teranes10.androidutils.extensions

import java.util.concurrent.ConcurrentHashMap

object ConcurrentHashMap {

    fun <K, V> ConcurrentHashMap<K, V>.nextBatch(batchSize: Int): List<V> {
        val batch = mutableListOf<V>()
        val keys = this.keys.iterator()
        repeat(batchSize) {
            if (keys.hasNext()) {
                val key = keys.next()
                this.remove(key)?.let { batch.add(it) }
            }
        }
        return batch
    }
}