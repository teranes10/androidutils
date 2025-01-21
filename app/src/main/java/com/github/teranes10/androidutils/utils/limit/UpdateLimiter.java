package com.github.teranes10.androidutils.utils.limit;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class UpdateLimiter {
    private long lastCallTime = 0;
    private Runnable lastUpdate = null;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> lastScheduledFuture = null;

    public UpdateLimiter() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized void schedule(Runnable update, long limit) {
        long now = System.currentTimeMillis();

        if (lastCallTime == 0 || now - lastCallTime >= limit) {
            update.run();
            lastCallTime = now;
        } else {
            lastUpdate = update;
            long delay = limit - (now - lastCallTime);

            if (lastScheduledFuture != null && !lastScheduledFuture.isDone()) {
                lastScheduledFuture.cancel(false);
            }

            lastScheduledFuture = scheduler.schedule(() -> {
                synchronized (UpdateLimiter.this) {
                    if (lastUpdate != null) {
                        lastUpdate.run();
                        lastUpdate = null;
                        lastCallTime = System.currentTimeMillis();
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
