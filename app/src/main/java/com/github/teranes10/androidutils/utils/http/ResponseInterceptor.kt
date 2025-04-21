package com.github.teranes10.androidutils.utils.http

import com.github.teranes10.androidutils.models.Outcome
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.charset.StandardCharsets

object ResponseInterceptor {

    fun interceptor(): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            val request = chain.request()
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) {
                    return@Interceptor response
                }

                val responseBodyString = response.body?.string().orEmpty()

                val (message, type) = when (response.code) {
                    400 -> "Invalid input. $responseBodyString" to Outcome.NetworkErrorType.InvalidInput
                    401 -> "Token expired. Please re-login. $responseBodyString" to Outcome.NetworkErrorType.Unauthorized
                    404 -> "Not found. $responseBodyString" to Outcome.NetworkErrorType.NotFound
                    500 -> "Server error. $responseBodyString" to Outcome.NetworkErrorType.ServerError
                    else -> "Something went wrong. Error code: ${response.code}. $responseBodyString" to Outcome.NetworkErrorType.Unknown
                }

                return@Interceptor createErrorResponse(request, message, type)
            } catch (e: Exception) {
                val message = e.localizedMessage ?: e.message ?: ""
                if (message.contains("Unable to resolve host")
                    || message.contains("No address associated with hostname")
                    || message.contains("timeout")
                ) {
                    return@Interceptor createErrorResponse(
                        request,
                        "No network connection to the server. Please check and retry.",
                        Outcome.NetworkErrorType.NoInternet
                    )
                }

                return@Interceptor createErrorResponse(request, "Something went wrong. $message", Outcome.NetworkErrorType.Unknown)
            }
        }
    }

    private fun createErrorResponse(request: Request, message: String?, type: Outcome.NetworkErrorType): Response {
        val result = Outcome.error(message.orEmpty(), networkType = type)
        val stringResult = Gson().toJson(result)

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message(message.orEmpty())
            .body(stringResult.toByteArray(StandardCharsets.UTF_8).toResponseBody("application/json".toMediaType()))
            .build()
    }
}
