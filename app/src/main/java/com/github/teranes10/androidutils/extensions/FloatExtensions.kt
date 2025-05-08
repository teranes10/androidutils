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
}