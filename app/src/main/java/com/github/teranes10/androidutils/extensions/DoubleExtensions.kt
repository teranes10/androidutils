package com.github.teranes10.androidutils.extensions

import java.text.NumberFormat
import java.util.Locale

class DoubleExtensions {

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
}