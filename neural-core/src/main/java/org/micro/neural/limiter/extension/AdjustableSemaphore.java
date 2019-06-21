package org.micro.neural.limiter.extension;

import java.util.concurrent.Semaphore;

/**
 * The Adjustable Semaphore.
 *
 * @author lry
 */
public class AdjustableSemaphore extends Semaphore {

    private static final long serialVersionUID = -392487128996569342L;

    /**
     * 最大许可数
     */
    private volatile int maxPermits = 0;

    public AdjustableSemaphore() {
        super(0);
    }

    public AdjustableSemaphore(int permits) {
        super(permits);
        this.setMaxPermits(permits);
    }

    public AdjustableSemaphore(int permits, boolean fair) {
        super(permits, fair);
        this.setMaxPermits(permits);
    }

    /**
     * 设置并发数(支持动态变更)
     *
     * @param maxPermits max permits
     */
    public synchronized void setMaxPermits(int maxPermits) {
        if (maxPermits < 1) {
            throw new IllegalArgumentException("Semaphore size(" + maxPermits + ") must be at least 1");
        }

        int delta = maxPermits - this.maxPermits;
        if (delta == 0) {
            return;
        } else if (delta > 0) {
            super.release(delta);
        } else {
            delta *= -1;
            super.reducePermits(delta);
        }

        this.maxPermits = maxPermits;
    }

}