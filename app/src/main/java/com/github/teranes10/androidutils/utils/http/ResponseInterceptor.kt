package com.github.teranes10.androidutils.utils.http

import com.github.teranes10.androidutils.models.Outcome.Companion.fail
import com.github.teranes10.androidutils.models.Outcome.OutcomeType
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.charset.StandardCharsets

object ResponseInterceptor {

    fun interceptor(customResponseHandler: ((responseCode: Int, responseBody: String) -> Pair<String, OutcomeType>?)? = null): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            val request = chain.request()
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) {
                    return@Interceptor response
                }

                val responseBodyString = response.body?.string().orEmpty()
                val customResponse = customResponseHandler?.invoke(response.code, responseBodyString)

                val (message, type) = customResponse
                    ?: when (response.code) {
                        400 -> "Invalid input. $responseBodyString" to OutcomeType.InvalidInput
                        401 -> "Token expired. Please re-login. $responseBodyString" to OutcomeType.Unauthorized
                        404 -> "Not found. $responseBodyString" to OutcomeType.NotFound
                        500 -> "Server error. $responseBodyString" to OutcomeType.ServerError
                        else -> "Something went wrong. Error code: ${response.code}. $responseBodyString" to OutcomeType.Unknown
                    }

                return@Interceptor createErrorResponse(request, message, type)
            } catch (e: Exception) {
                val message = e.localizedMessage ?: e.message ?: ""
                val customResponse = customResponseHandler?.invoke(-1, message)
                if (customResponse != null) {
                    return@Interceptor createErrorResponse(request, customResponse.first, customResponse.second)
                }

                if (message.contains("Unable to resolve host")
                    || message.contains("No address associated with hostname")
                    || message.contains("timeout")
                ) {
                    return@Interceptor createErrorResponse(
                        request,
                        "No network connection to the server. Please check and retry.",
                        OutcomeType.NoInternet
                    )
                }

                return@Interceptor createErrorResponse(request, "Something went wrong. $message", OutcomeType.Unknown)
            }
        }
    }

    private fun createErrorResponse(request: Request, message: String?, type: OutcomeType): Response {
        val result = fail<Any>(message.orEmpty(), type)
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
