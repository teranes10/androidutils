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

    fun interceptor(): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            val request = chain.request()
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) {
                    return@Interceptor response
                }

                val responseBodyString = response.body?.string().orEmpty()

                return@Interceptor when (response.code) {
                    400 -> createErrorResponse(request, "Invalid input. $responseBodyString", OutcomeType.InvalidInput)
                    401 -> createErrorResponse(request, "Token expired. Please re login. $responseBodyString", OutcomeType.Unauthorized)
                    404 -> createErrorResponse(request, "Not found. $responseBodyString", OutcomeType.NotFound)
                    else -> createErrorResponse(
                        request, "Something went wrong. Error code: ${response.code}. $responseBodyString", OutcomeType.Unknown
                    )
                }
            } catch (e: Exception) {
                return@Interceptor errorHandler(request, e)
            }
        }
    }

    private fun errorHandler(request: Request, e: Exception): Response {
        val message = e.localizedMessage ?: e.message ?: ""
        if (message.contains("Unable to resolve host") || message.contains("No address associated with hostname") || message.contains("timeout")) {
            return createErrorResponse(
                request, "No network connection to the server. Please check and retry.", OutcomeType.NoInternet
            )
        }

        return createErrorResponse(request, "Something went wrong. $message", OutcomeType.Unknown)
    }

    private fun createErrorResponse(request: Request, message: String?, type: OutcomeType): Response {
        val result = fail<Any>(message.orEmpty(), type)
        val stringResult = Gson().toJson(result)

        val responseBuilder = Response.Builder()
        responseBuilder.request(request)
        responseBuilder.protocol(Protocol.HTTP_1_1)
        responseBuilder.code(200)
        responseBuilder.message(message.orEmpty())

        val mediaType = "application/json".toMediaType()
        val responseBody = stringResult.toByteArray(StandardCharsets.UTF_8).toResponseBody(mediaType)
        return responseBuilder.body(responseBody).build()
    }
}
