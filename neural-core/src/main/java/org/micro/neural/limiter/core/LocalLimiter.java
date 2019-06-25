package org.micro.neural.limiter.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.LimiterConfig;
import org.micro.neural.limiter.LimiterGlobalConfig;
import org.micro.neural.limiter.LimiterGlobalConfig.*;
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

    private LongAdder counter;
    private AdjustableSemaphore semaphore;

    @Override
    public void refresh(LimiterGlobalConfig limiterGlobalConfig) {
        super.refresh(limiterGlobalConfig);

        // rate limiter
        if (LocalRate.RATE_LIMITER == limiterGlobalConfig.getLocalRate()) {
            rateLimiter = AdjustableRateLimiter.create(1);
        } else {
            CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
            cacheBuilder.expireAfterWrite(0, TimeUnit.SECONDS);
            cache = cacheBuilder.build();
        }

        // concurrent limiter
        if (LocalConcurrent.SEMAPHORE == limiterGlobalConfig.getLocalConcurrent()) {
            semaphore = new AdjustableSemaphore(1, true);
        } else {
            counter = new LongAdder();
        }
    }

    @Override
    public synchronized boolean refresh(LimiterConfig limiterConfig) throws Exception {
        try {
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
        return LocalConcurrent.SEMAPHORE == limiterGlobalConfig.getLocalConcurrent()
                ? incrementSemaphore() : incrementCASCell();
    }

    @Override
    protected void decrementConcurrent() {
        if (LocalConcurrent.SEMAPHORE == limiterGlobalConfig.getLocalConcurrent()) {
            semaphore.release();
        } else {
            counter.decrement();
        }
    }

    @Override
    protected Acquire tryAcquireRate() {
        return LocalRate.RATE_LIMITER == limiterGlobalConfig.getLocalRate()
                ? tryAcquireRateLimiter() : tryAcquireCASCellCache();
    }

    @Override
    protected Acquire tryAcquireRequest() {
        return null;
    }

    /**
     * The increment Semaphore
     *
     * @return {@link Acquire}
     */
    private Acquire incrementSemaphore() {
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

    /**
     * The increment CAS cell
     *
     * @return {@link Acquire}
     */
    private Acquire incrementCASCell() {
        if (limiterConfig.getMaxConcurrent() > counter.longValue()) {
            counter.increment();
            return Acquire.SUCCESS;
        }

        return Acquire.FAILURE;
    }

    /**
     * The try acquire rate limiter
     *
     * @return {@link Acquire}
     */
    private Acquire tryAcquireRateLimiter() {
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

    /**
     * The try acquire CAS cell cache
     *
     * @return {@link Acquire}
     */
    private Acquire tryAcquireCASCellCache() {
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
