package cn.micro.neural.limiter.core;

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

    // ==== counter limiter

    private Cache<Long, LongAdder> cache;

    @Override
    protected boolean tryRefresh(LimiterConfig limiterConfig) {
        // rate limiter
        this.rateLimiter = AdjustableRateLimiter.create(limiterConfig.getRate().getMaxRate());
        // concurrent limiter
        this.semaphore = new AdjustableSemaphore(limiterConfig.getConcurrent().getMaxPermit(), true);
        // counter limiter
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        cacheBuilder.expireAfterWrite(limiterConfig.getCounter().getInterval().toMillis(), TimeUnit.MILLISECONDS);
        this.cache = cacheBuilder.build();

        // the refresh rateLimiter
        rateLimiter.setRate(limiterConfig.getRate().getRateUnit());
        // the refresh semaphore
        semaphore.setMaxPermits(limiterConfig.getConcurrent().getMaxPermit());
        return true;
    }

    @Override
    protected Acquire tryAcquireConcurrent() {
        LimiterConfig.ConcurrentLimiterConfig concurrent = config.getConcurrent();
        try {
            if (concurrent.getTimeout() > 0) {
                // try acquire by timeout
                return semaphore.tryAcquire(concurrent.getPermitUnit(),
                        concurrent.getTimeout(), TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            }

            // try acquire
            return semaphore.tryAcquire(concurrent.getPermitUnit()) ? Acquire.SUCCESS : Acquire.FAILURE;
        } catch (Exception e) {
            log.error("The try acquire local concurrent is exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected void releaseConcurrent() {
        semaphore.release();
    }

    @Override
    protected Acquire tryAcquireRate() {
        try {
            Long timeout = config.getRate().getTimeout();
            if (timeout > 0) {
                // try acquire by timeout
                return rateLimiter.tryAcquire(timeout, TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            }

            // try acquire
            return rateLimiter.tryAcquire() ? Acquire.SUCCESS : Acquire.FAILURE;
        } catch (Exception e) {
            log.error("The try acquire local rate limiter is exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireCounter() {
        LimiterConfig.CounterLimiterConfig counter = config.getCounter();
        try {
            Long key = System.currentTimeMillis() / counter.getInterval().toMillis();
            LongAdder counterLongAdder = cache.get(key, LongAdder::new);
            counterLongAdder.add(counter.getCountUnit());
            if (counter.getMaxCount() > counterLongAdder.longValue()) {
                return Acquire.SUCCESS;
            }

            return Acquire.FAILURE;
        } catch (Exception e) {
            log.error("The try acquire memory rate limiter is exception", e);
            return Acquire.EXCEPTION;
        }
    }

}
