package com.github.teranes10.androidutils.utils.http;

import com.github.teranes10.androidutils.models.Outcome;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ResponseInterceptor {
    public static Interceptor interceptor() {
        return chain -> {
            Request request = chain.request();
            try {
                Response response = chain.proceed(request);
                if (response.isSuccessful()) {
                    return response;
                }

                return switch (response.code()) {
                    case 400 -> createErrorResponse(request, "Invalid input.", Outcome.OutcomeType.InvalidInput);
                    case 401 -> createErrorResponse(request, "Token expired. Please re login.", Outcome.OutcomeType.Unauthorized);
                    case 404 -> createErrorResponse(request, "Not found.", Outcome.OutcomeType.NotFound);
                    default ->
                            createErrorResponse(request, "Something went wrong. Error code:" + response.code(), Outcome.OutcomeType.Unknown);
                };
            } catch (Exception e) {
                return errorHandler(request, e);
            }
        };
    }

    private static Response errorHandler(Request request, Exception e) {
        String message = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "";
        if (message.contains("Unable to resolve host") || message.contains("No address associated with hostname") || message.contains("timeout")) {
            return createErrorResponse(request, "No network connection to the server. Please check and retry.", Outcome.OutcomeType.NoInternetConnection);
        }

        return createErrorResponse(request, "Something went wrong. " + message, Outcome.OutcomeType.Unknown);
    }

    private static Response createErrorResponse(Request request, String message, Outcome.OutcomeType type) {
        message = message != null ? message : "";

        Outcome<Object> result = Outcome.Companion.fail(message, type);
        String stringResult = new Gson().toJson(result);

        Response.Builder responseBuilder = new Response.Builder();
        responseBuilder.request(request);
        responseBuilder.protocol(Protocol.HTTP_1_1);
        responseBuilder.code(200);
        responseBuilder.message(message);

        return responseBuilder.body(ResponseBody.create(
                stringResult.getBytes(StandardCharsets.UTF_8),
                MediaType.parse("application/json"))).build();
    }
}
