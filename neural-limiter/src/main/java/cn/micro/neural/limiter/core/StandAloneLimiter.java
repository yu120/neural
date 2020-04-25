package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.EventType;
import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.extension.AdjustableRateLimiter;
import cn.micro.neural.limiter.extension.AdjustableSemaphore;
import cn.neural.common.extension.Extension;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

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
        try {
            super.refresh(limiterConfig);

            // rate limiter
            this.rateLimiter = AdjustableRateLimiter.create(limiterConfig.getRate().getMaxRate());
            // concurrent limiter
            this.semaphore = new AdjustableSemaphore(limiterConfig.getConcurrent().getMaxPermit(), true);
            // request limiter
            CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
            cacheBuilder.expireAfterWrite(limiterConfig.getRequest().getInterval().toMillis(), TimeUnit.MILLISECONDS);
            this.cache = cacheBuilder.build();

            // the refresh rateLimiter
            rateLimiter.setRate(limiterConfig.getRate().getRateUnit());
            // the refresh semaphore
            semaphore.setMaxPermits(limiterConfig.getConcurrent().getMaxPermit());
            return true;
        } catch (Exception e) {
            super.collectEvent(EventType.REFRESH_EXCEPTION);
            throw e;
        }
    }

    @Override
    protected Acquire incrementConcurrent() {
        LimiterConfig.ConcurrentLimiterConfig concurrent = config.getConcurrent();

        try {
            // the get concurrent timeout
            if (concurrent.getTimeout() > 0) {
                // the try acquire by timeout
                return semaphore.tryAcquire(concurrent.getPermitUnit(),
                        concurrent.getTimeout(), TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            } else {
                // the try acquire
                return semaphore.tryAcquire(concurrent.getPermitUnit()) ? Acquire.SUCCESS : Acquire.FAILURE;
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
            Long timeout = config.getRate().getTimeout();
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
            if (config.getRate().getRateUnit() > times.longValue()) {
                return Acquire.SUCCESS;
            }
        } catch (Exception e) {
            log.error("The try acquire memory rate limiter is exception", e);
            return Acquire.EXCEPTION;
        }

        return Acquire.FAILURE;
    }

}
