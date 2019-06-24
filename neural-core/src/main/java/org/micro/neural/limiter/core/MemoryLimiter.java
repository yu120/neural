package org.micro.neural.limiter.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.LimiterConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * The limiter based on memory implementation(second level).
 *
 * @author lry
 */
@Slf4j
@Extension("memory")
public class MemoryLimiter extends AbstractCallLimiter {

    private final LongAdder concurrencyCounter = new LongAdder();
    private Cache<Long, LongAdder> cache;

    @Override
    public synchronized boolean refresh(LimiterConfig limiterConfig) throws Exception {
        if (super.refresh(limiterConfig)) {
            return false;
        }

        // The core limiter granularity is SECOND
        LimiterConfig config = super.limiterConfig;
        if (null == limiterConfig.getGranularity() || LimiterConfig.Unit.SEC != config.getUnit()) {
            return false;
        }

        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        cacheBuilder.expireAfterWrite(config.getRate(), TimeUnit.SECONDS);
        cache = cacheBuilder.build();
        return true;
    }

    @Override
    protected Acquire tryAcquireConcurrency() {
        if (limiterConfig.getConcurrency() > concurrencyCounter.longValue()) {
            concurrencyCounter.increment();
            return Acquire.SUCCESS;
        }

        return Acquire.FAILURE;
    }

    @Override
    protected void releaseAcquireConcurrency() {
        concurrencyCounter.decrement();
    }

    @Override
    protected Acquire tryAcquireRequest() {
        return null;
    }

    @Override
    protected Acquire tryAcquireRateLimiter() {
        if (cache == null) {
            return Acquire.SUCCESS;
        }
        
        try {
            LongAdder times = cache.get(System.currentTimeMillis() / 1000, LongAdder::new);
            if (limiterConfig.getRate() > times.longValue()) {
                return Acquire.SUCCESS;
            }
        } catch (Exception e) {
            log.error("The try acquire memory rate limiter is exception", e);
            return Acquire.EXCEPTION;
        }

        return Acquire.FAILURE;
    }

}
