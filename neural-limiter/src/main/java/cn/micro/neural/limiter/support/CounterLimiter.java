package cn.micro.neural.limiter.support;

import cn.micro.neural.limiter.ILimiter;
import cn.micro.neural.limiter.LimiterConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CountLimiter
 *
 * @author lry
 */
public class CounterLimiter implements ILimiter {

    private LimiterConfig limiterConfig;
    private final ConcurrentMap<String, CounterLimiterInfo> counters = new ConcurrentHashMap<>();

    @Override
    public void initialize(LimiterConfig limiterConfig) throws Exception {
        this.limiterConfig = limiterConfig;
    }

    @Override
    public boolean callRate(String key, long maxLimit, long limitPeriod) {
        CounterLimiterInfo limiter = counters.get(key);
        if (limiter == null) {
            counters.put(key, limiter = new CounterLimiterInfo());
        }

        AtomicLong counter = limiter.getCounter();
        counter.addAndGet(1);

        // 超过了间隔时间，直接重新开始计数
        if (System.currentTimeMillis() - limiter.getStartTime() > limitPeriod) {
            limiter.setStartTime(System.currentTimeMillis());
            counter.set(1);
            return true;
        }

        // 还在间隔时间内,check有没有超过限流的个数
        return counter.get() <= maxLimit;
    }

    @Override
    public void destroy() {
        counters.clear();
    }

    @Data
    public static class CounterLimiterInfo implements Serializable {
        /**
         * Limit counter
         */
        private final AtomicLong counter = new AtomicLong(0);
        /**
         * Start time
         */
        private long startTime = System.currentTimeMillis();
    }

}
