package com.github.teranes10.androidutils.utils;

import android.app.Application;

public class MyApplication extends Application {
    public void setupDefaultConnectionUtil(String url, int intervalInMillis) {
        DefaultConnectionUtil.setup(url, intervalInMillis);
    }
}
