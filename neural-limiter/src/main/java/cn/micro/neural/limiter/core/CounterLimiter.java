package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterContext;
import cn.micro.neural.limiter.LimiterStatistics;
import cn.micro.neural.limiter.OriginalCall;

import java.util.concurrent.atomic.AtomicLong;

/**
 * CountLimiter
 *
 * @author lry
 */
public class CounterLimiter implements ILimiter {

    private LimiterConfig limiterConfig;
    /**
     * Limit counter
     */
    private final AtomicLong counter = new AtomicLong(0);
    /**
     * Start time
     */
    private long startTime = System.currentTimeMillis();

    @Override
    public boolean refresh(LimiterConfig limiterConfig) throws Exception {
        this.limiterConfig = limiterConfig;
        return false;
    }

    @Override
    public Object wrapperCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        counter.addAndGet(1);

        // 超过了间隔时间，直接重新开始计数
        if (System.currentTimeMillis() - startTime > limiterConfig.getRateTimeout()) {
            startTime = System.currentTimeMillis();
            counter.set(1);
            return true;
        }

        // 还在间隔时间内,check有没有超过限流的个数
        return counter.get() <= limiterConfig.getMaxPermitRate();
    }

    @Override
    public LimiterStatistics getStatistics() {
        return null;
    }

}

