package com.crm.rdvision.utility;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RateLimiter {

    private final Semaphore semaphore;
    private final long timeout;

    public RateLimiter(int permits, long timeout) {
        this.semaphore = new Semaphore(permits);
        this.timeout = timeout;
    }

    public boolean tryAcquire() throws InterruptedException {
        return semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    public void release() {
        semaphore.release();
    }
}
