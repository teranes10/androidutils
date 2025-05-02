package com.github.teranes10.androidutils.extensions

object StringExtensions {

    fun String.truncate(maxLength: Int): String {
        return if (this.length > maxLength) this.take(maxLength - 2) + ".." else this
    }

    fun String.trimSpace(): String = trim { it.isWhitespace() }

    fun String.sanitizeForSMS(): String {
        return this
            .replace(Regex("[\\n\\r\\t]"), " ")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^\\x20-\\x7E]"), "")
            .trim()
    }
}