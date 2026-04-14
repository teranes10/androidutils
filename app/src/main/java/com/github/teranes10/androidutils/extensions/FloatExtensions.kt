package com.github.teranes10.androidutils.extensions

import kotlin.math.round

object FloatExtensions {

    fun Float.toFixed2(): Float = round(this * 100) / 100

    fun Float.safeDiv(divisor: Float, fallback: Float = 0f): Float =
        if (divisor.isFinite() && divisor != 0f) this / divisor else fallback

    fun Float.safeDiv(divisor: Double, fallback: Double = 0.0): Double =
        if (divisor.isFinite() && divisor != 0.0) this / divisor else fallback

    fun Float.safeDiv(divisor: Int, fallback: Float = 0f): Float =
        if (divisor != 0) this / divisor else fallback

    fun Float.safeDiv(divisor: Long, fallback: Float = 0f): Float =
        if (divisor != 0L) this / divisor else fallback

    fun Float.isPositive(): Boolean {
        return this > 0.0 && this.isFinite()
    }

    fun Collection<Float>.median(): Float? {
        if (isEmpty()) return 0f

        val clean = this.filter { it.isFinite() }

        if (clean.isEmpty()) return 0f

        val sorted = clean.sorted()
        val mid = sorted.size / 2

        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2f
        } else {
            sorted[mid]
        }
    }
}