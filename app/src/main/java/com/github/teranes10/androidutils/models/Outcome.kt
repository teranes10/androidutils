package com.github.teranes10.androidutils.models

data class Outcome<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = "",
    val networkType: NetworkErrorType?,
    val outcomeType: OutcomeType?,
) {

    val failure get() = !success

    val type: OutcomeType get() = outcomeType ?: OutcomeType.fromEnum(networkType ?: NetworkErrorType.Unknown)

    companion object {
        fun <T> ok(data: T?, message: String = "", type: OutcomeType? = null, networkType: NetworkErrorType? = null): Outcome<T> {
            return Outcome(success = true, data = data, message = message, outcomeType = type, networkType = networkType)
        }

        fun <T> fail(message: String, type: OutcomeType? = null, networkType: NetworkErrorType? = null, data: T? = null): Outcome<T> {
            return Outcome(success = false, message = message, outcomeType = type, networkType = networkType, data = data)
        }

        fun error(message: String, type: OutcomeType? = null, networkType: NetworkErrorType? = null): Outcome<*> {
            return Outcome<Any>(success = false, message = message, outcomeType = type, networkType = networkType)
        }
    }

    enum class NetworkErrorType {
        Unknown,
        InvalidInput,
        Unauthorized,
        NotFound,
        ServerError,
        NoInternet
    }

    open class OutcomeType(val message: String) {
        data object Unknown : OutcomeType("Unknown")
        data object InvalidInput : OutcomeType("Invalid Input")
        data object Unauthorized : OutcomeType("Unauthorized")
        data object NotFound : OutcomeType("Not found")
        data object ServerError : OutcomeType("Server error.")
        data object NoInternet : OutcomeType("No Internet")

        companion object {
            fun fromEnum(type: NetworkErrorType): OutcomeType = when (type) {
                NetworkErrorType.Unknown -> Unknown
                NetworkErrorType.InvalidInput -> InvalidInput
                NetworkErrorType.Unauthorized -> Unauthorized
                NetworkErrorType.NotFound -> NotFound
                NetworkErrorType.ServerError -> ServerError
                NetworkErrorType.NoInternet -> NoInternet
            }
        }
    }
}