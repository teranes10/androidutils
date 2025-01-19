package com.github.teranes10.androidutils.utils.limit;

public class Throttle {
    private long lastExecutedTime = 0;

    public void throttle(Runnable action, long intervalMillis) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExecutedTime >= intervalMillis) {
            lastExecutedTime = currentTime;
            action.run();
        }
    }
}
