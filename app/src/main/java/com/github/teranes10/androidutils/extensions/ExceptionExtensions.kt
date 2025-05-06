package com.github.teranes10.androidutils.extensions

object ExceptionExtensions {

    val Exception.displayMessage get() = this.localizedMessage ?: this.message ?: "Something went wrong."
}