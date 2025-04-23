package com.github.teranes10.androidutils.extensions

object StringExtensions {

    fun String.truncate(maxLength: Int): String {
        return if (this.length > maxLength) this.take(maxLength) + "…" else this
    }

    fun String.trimSpace(): String = trim { it.isWhitespace() }
}