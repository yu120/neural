package org.micro.neural.limiter.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.LimiterConfig;
import org.micro.neural.limiter.LimiterGlobalConfig;
import org.micro.neural.limiter.extension.AdjustableRateLimiter;
import org.micro.neural.limiter.extension.AdjustableSemaphore;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * The limiter based on adjustable's semaphore and rateLimiter implementation.
 *
 * @author lry
 * @apiNote The local limiter
 */
@Slf4j
@Extension("local")
public class LocalLimiter extends AbstractCallLimiter {

    // === rate limiter

    private Cache<Long, LongAdder> cache;
    private AdjustableRateLimiter rateLimiter;

    // === concurrent limiter

    private LongAdder concurrentCounter;
    private AdjustableSemaphore semaphore;

    @Override
    public void initialize(LimiterGlobalConfig limiterGlobalConfig) {
        super.initialize(limiterGlobalConfig);

        // rate limiter
        if (LimiterGlobalConfig.LocalRate.RATE_LIMITER == limiterGlobalConfig.getLocalRate()) {
            rateLimiter = AdjustableRateLimiter.create(1);
        } else if (LimiterGlobalConfig.LocalRate.CACHE == limiterGlobalConfig.getLocalRate()) {
            CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
            cacheBuilder.expireAfterWrite(0, TimeUnit.SECONDS);
            cache = cacheBuilder.build();
        }

        // concurrent limiter
        if (LimiterGlobalConfig.LocalConcurrent.SEMAPHORE == limiterGlobalConfig.getLocalConcurrent()) {
            semaphore = new AdjustableSemaphore(1, true);
        } else if (LimiterGlobalConfig.LocalConcurrent.LONG_ADDER == limiterGlobalConfig.getLocalConcurrent()) {
            concurrentCounter = new LongAdder();
        }
    }

    @Override
    public synchronized boolean refresh(LimiterConfig limiterConfig) throws Exception {
        try {
            LimiterGlobalConfig limiterGlobalConfig = super.getLimiterGlobalConfig();
            LimiterConfig config = super.getLimiterConfig();
            if (0 < config.getMaxConcurrent()) {
                // the refresh semaphore
                semaphore.setMaxPermits(config.getMaxConcurrent().intValue());
            }
            if (0 < config.getRatePermit()) {
                // the refresh rateLimiter
                rateLimiter.setRate(config.getRatePermit());
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
                return semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            } else {
                // the try acquire
                return semaphore.tryAcquire() ? Acquire.SUCCESS : Acquire.FAILURE;
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
        return null;
    }

}
