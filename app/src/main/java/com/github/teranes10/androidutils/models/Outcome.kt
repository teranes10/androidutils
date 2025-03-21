package com.github.teranes10.androidutils.models

data class Outcome<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = "",
    val type: OutcomeType = OutcomeType.Unknown
) {

    val isSuccess get() = success
    val isFailure get() = !success

    companion object {
        fun <T> ok(data: T, message: String = ""): Outcome<T> {
            return Outcome(success = true, data = data, message = message)
        }

        fun <T> fail(message: String, type: OutcomeType = OutcomeType.Unknown): Outcome<T> {
            return Outcome(success = false, message = message, type = type)
        }

        fun <T> fail(data: T?, message: String, type: OutcomeType = OutcomeType.Unknown): Outcome<T> {
            return Outcome(success = false, data = data, message = message, type = type)
        }
    }

    enum class OutcomeType {
        InvalidInput,
        Unauthorized,
        NotFound,
        Unknown,
        NoInternetConnection
    }
}