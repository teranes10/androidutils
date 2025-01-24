package com.github.teranes10.androidutils.utils.http;

import okhttp3.logging.HttpLoggingInterceptor;

public class LoggingInterceptor {
    public static HttpLoggingInterceptor interceptor() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return loggingInterceptor;
    }
}
