@file:OptIn(ExperimentalContracts::class)

package com.github.teranes10.androidutils.extensions

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object LongExtensions {

    fun Long?.isNullOrZero(): Boolean {
        contract { returns(false) implies (this@isNullOrZero != null) }
        return this == null || this == 0L
    }

    fun Long?.isNullOrNonPositive(): Boolean {
        contract { returns(false) implies (this@isNullOrNonPositive != null) }
        return this == null || this <= 0
    }


    fun Long.safeDiv(divisor: Long, fallback: Long = 0): Long =
        if (divisor != 0L) this / divisor else fallback

    fun Long.safeDiv(divisor: Int, fallback: Long = 0): Long =
        if (divisor != 0) this / divisor else fallback

    fun Long.safeDiv(divisor: Float, fallback: Float = 0f): Float =
        if (divisor.isFinite() && divisor != 0f) this / divisor else fallback

    fun Long.safeDiv(divisor: Double, fallback: Double = 0.0): Double =
        if (divisor.isFinite() && divisor != 0.0) this / divisor else fallback

}