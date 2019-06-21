package org.micro.neural.limiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 支持过期的计数器
 *
 * @author lry
 */
public class CacheCounter {

    private volatile int maxPermits = 0;
    private LoadingCache<Long, AtomicLong> counter;

    public CacheCounter(long duration, TimeUnit unit) {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, unit);
        counter = cacheBuilder.build(new CacheLoader<Long, AtomicLong>() {
            @Override
            public AtomicLong load(Long seconds) throws Exception {
                return new AtomicLong(0);
            }
        });
    }

    public synchronized void setMaxPermit(int newMax) {
        if (newMax < 0) {
            throw new IllegalArgumentException("Counter size must be at least 1," + " was " + newMax);
        }
        this.maxPermits = newMax;
    }

    public boolean tryAcquire() throws Throwable {
        long currentSeconds = System.currentTimeMillis() / 1000;
        return counter.get(currentSeconds).incrementAndGet() > maxPermits ? false : true;
    }

}
