package org.micro.neural.limiter.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.micro.neural.extension.Extension;
import org.micro.neural.limiter.LimiterConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * The limiter based on memory implementation.
 *
 * @author lry
 */
@Slf4j
@Extension("memory")
public class MemoryLimiter extends AbstractCallLimiter {

    private final LongAdder concurrencyCounter = new LongAdder();
    private final CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
    private LoadingCache<Long, LongAdder> loadingCache;

    @Override
    public synchronized boolean refresh(LimiterConfig limiterConfig) throws Exception {
        if (super.refresh(limiterConfig)) {
            return false;
        }

        // The core limiter granularity is SECOND
        LimiterConfig config = super.getLimiterConfig();
        if (null == config.getGranularity() || LimiterConfig.Unit.SEC != config.getUnit()) {
            return false;
        }

        cacheBuilder.expireAfterWrite(config.getRate(), TimeUnit.SECONDS);
        if (null == loadingCache) {
            loadingCache = cacheBuilder.build(new CacheLoader<Long, LongAdder>() {
                @Override
                public LongAdder load(Long seconds) throws Exception {
                    return new LongAdder();
                }
            });
        }

        return true;
    }

    @Override
    protected Acquire tryAcquireConcurrency() {
        if (super.getLimiterConfig().getConcurrency() > concurrencyCounter.longValue()) {
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
    protected Acquire tryAcquireRateLimiter() {
        try {
            LongAdder number = loadingCache.get(System.currentTimeMillis() / 1000);
            if (super.getLimiterConfig().getRate() > number.longValue()) {
                return Acquire.SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Acquire.FAILURE;
    }

}
