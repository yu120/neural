package org.micro.neural.limiter.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.LimiterConfig;
import org.micro.neural.limiter.extension.AdjustableRateLimiter;
import org.micro.neural.limiter.extension.AdjustableSemaphore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * The Stand Alone Limiter.
 *
 * @author lry
 * @apiNote The local limiter
 */
@Slf4j
@Extension("stand-alone")
public class StandAloneLimiter extends AbstractCallLimiter {

    // === rate limiter

    private AdjustableRateLimiter rateLimiter;

    // === concurrent limiter

    private AdjustableSemaphore semaphore;

    // ==== request limiter

    private Cache<Long, LongAdder> cache;

    @Override
    public synchronized boolean refresh(LimiterConfig limiterConfig) throws Exception {
        super.refresh(limiterConfig);

        // rate limiter
        rateLimiter = AdjustableRateLimiter.create(limiterConfig.getMaxPermitRate());

        // request limiter
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        cacheBuilder.expireAfterWrite(limiterConfig.getRequestInterval().toMillis(), TimeUnit.MILLISECONDS);
        cache = cacheBuilder.build();

        // concurrent limiter
        semaphore = new AdjustableSemaphore(limiterConfig.getMaxPermitConcurrent(), true);

        try {
            if (0 < limiterConfig.getMaxPermitConcurrent()) {
                // the refresh semaphore
                semaphore.setMaxPermits(limiterConfig.getMaxPermitConcurrent());
            }
            if (0 < limiterConfig.getRatePermit()) {
                // the refresh rateLimiter
                rateLimiter.setRate(limiterConfig.getRatePermit());
            }

            return true;
        } catch (Exception e) {
            log.error("The refresh local limiter is exception", e);
        }

        return false;
    }

    @Override
    protected Acquire incrementConcurrent() {
        try {
            // the get concurrent timeout
            Long timeout = limiterConfig.getConcurrentTimeout();
            if (timeout > 0) {
                // the try acquire by timeout
                return semaphore.tryAcquire(limiterConfig.getConcurrentPermit(),
                        timeout, TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            } else {
                // the try acquire
                return semaphore.tryAcquire(limiterConfig.getConcurrentPermit()) ? Acquire.SUCCESS : Acquire.FAILURE;
            }
        } catch (Exception e) {
            log.error("The try acquire local concurrent is exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void decrementConcurrent() {
        semaphore.release();
    }

    @Override
    protected Acquire tryAcquireRate() {
        try {
            // the get rate timeout
            Long timeout = limiterConfig.getRateTimeout();
            if (timeout > 0) {
                // the try acquire by timeout
                return rateLimiter.tryAcquire(timeout, TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            } else {
                // the try acquire
                return rateLimiter.tryAcquire() ? Acquire.SUCCESS : Acquire.FAILURE;
            }
        } catch (Exception e) {
            log.error("The try acquire local rate limiter is exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireRequest() {
        if (cache == null) {
            return Acquire.SUCCESS;
        }

        try {
            LongAdder times = cache.get(System.currentTimeMillis() / 1000, LongAdder::new);
            if (limiterConfig.getRatePermit() > times.longValue()) {
                return Acquire.SUCCESS;
            }
        } catch (Exception e) {
            log.error("The try acquire memory rate limiter is exception", e);
            return Acquire.EXCEPTION;
        }

        return Acquire.FAILURE;
    }

}
