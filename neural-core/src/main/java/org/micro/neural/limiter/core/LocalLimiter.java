package org.micro.neural.limiter.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.LimiterGlobalConfig;
import org.micro.neural.limiter.extension.AdjustableRateLimiter;
import org.micro.neural.limiter.extension.AdjustableSemaphore;
import org.micro.neural.limiter.LimiterConfig;
import lombok.extern.slf4j.Slf4j;

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
    private final AdjustableRateLimiter rateLimiter = AdjustableRateLimiter.create(1);

    // === concurrent limiter

    private final LongAdder concurrentCounter = new LongAdder();
    private final AdjustableSemaphore semaphore = new AdjustableSemaphore(1, true);

    @Override
    public synchronized boolean doRefresh(LimiterConfig limiterConfig) throws Exception {
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

            CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
            cacheBuilder.expireAfterWrite(config.getRatePermit(), TimeUnit.SECONDS);
            cache = cacheBuilder.build();

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
