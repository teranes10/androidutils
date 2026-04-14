package com.github.teranes10.androidutils.extensions

import java.text.NumberFormat
import java.util.Locale

object DoubleExtensions {

    fun Double.toCurrency(
        language: String = "en",
        country: String = "AU",
        includeSymbol: Boolean = true,
        decimalPlaces: Int = 2
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale(language, country)).apply {
            maximumFractionDigits = decimalPlaces
            minimumFractionDigits = decimalPlaces
        }

        val formatted = formatter.format(this)
        return if (includeSymbol) formatted else formatted.replace(Regex("[^\\d.,]"), "").trim()
    }

    fun Double.safeDiv(divisor: Double, fallback: Double = 0.0): Double =
        if (divisor.isFinite() && divisor != 0.0) this / divisor else fallback

    fun Double.safeDiv(divisor: Float, fallback: Double = 0.0): Double =
        if (divisor.isFinite() && divisor != 0f) this / divisor else fallback

    fun Double.safeDiv(divisor: Int, fallback: Double = 0.0): Double =
        if (divisor != 0) this / divisor else fallback

    fun Double.safeDiv(divisor: Long, fallback: Double = 0.0): Double =
        if (divisor != 0L) this / divisor else fallback

    fun Double.isPositive(): Boolean {
        return this > 0.0 && this.isFinite()
    }

    fun Collection<Double>.median(): Double {
        if (isEmpty()) return 0.0

        val clean = this.filter { it.isFinite() }

        if (clean.isEmpty()) return 0.0

        val sorted = clean.sorted()
        val mid = sorted.size / 2

        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2f
        } else {
            sorted[mid]
        }
    }
}