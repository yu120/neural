package org.micro.neural.limiter.core;

import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.extension.AdjustableRateLimiter;
import org.micro.neural.limiter.extension.AdjustableSemaphore;
import org.micro.neural.limiter.LimiterConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * The limiter based on adjustable's semaphore and rateLimiter implementation.
 *
 * @author lry
 * @apiNote The local limiter
 */
@Slf4j
@Extension("local")
public class LocalLimiter extends AbstractCallLimiter {

    private final AdjustableRateLimiter rateLimiter = AdjustableRateLimiter.create(1);
    private final AdjustableSemaphore semaphore = new AdjustableSemaphore(1, true);

    @Override
    public synchronized boolean refresh(LimiterConfig limiterConfig) throws Exception {
        if (super.refresh(limiterConfig)) {
            try {
                LimiterConfig config = super.getLimiterConfig();
                if (0 < config.getConcurrency()) {
                    // the refresh semaphore
                    semaphore.setMaxPermits(config.getConcurrency().intValue());
                }
                if (0 < config.getRate()) {
                    // the refresh rateLimiter
                    rateLimiter.setRate(config.getRate());
                }

                return true;
            } catch (Exception e) {
                log.error("The refresh LocalLimiter is exception", e);
            }
        }

        return false;
    }

    @Override
    protected Acquire tryAcquireConcurrency() {
        try {
            // the get concurrency timeout
            Long timeout = super.getLimiterConfig().getConcurrencyTimeout();
            if (timeout > 0) {
                // the try acquire by timeout
                return semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            } else {
                // the try acquire
                return semaphore.tryAcquire() ? Acquire.SUCCESS : Acquire.FAILURE;
            }
        } catch (Exception e) {
            log.error("The try acquire concurrency is exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void releaseAcquireConcurrency() {
        semaphore.release();
    }

    @Override
    protected Acquire tryAcquireRateLimiter() {
        try {
            // the get rate timeout
            Long timeout = super.getLimiterConfig().getRateTimeout();
            if (timeout > 0) {
                // the try acquire by timeout
                return rateLimiter.tryAcquire(timeout, TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            } else {
                // the try acquire
                return rateLimiter.tryAcquire() ? Acquire.SUCCESS : Acquire.FAILURE;
            }
        } catch (Exception e) {
            log.error("The try acquire rate limiter is exception", e);
            return Acquire.EXCEPTION;
        }
    }

}
