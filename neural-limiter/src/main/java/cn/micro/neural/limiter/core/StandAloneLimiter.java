package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.extension.AdjustableRateLimiter;
import cn.micro.neural.limiter.extension.AdjustableSemaphore;
import cn.neural.common.extension.Extension;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Stand Alone Limiter.
 * <p>
 * 1.The rate limiter：{@link AdjustableRateLimiter}
 * 2.The concurrent limiter：{@link AdjustableSemaphore}
 * 3.The counter limiter：{@link LoadingCache}
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

    private LoadingCache<Long, AtomicLong> counter;

    @Override
    protected boolean tryRefresh(LimiterConfig config) {
        // rate limiter
        this.rateLimiter = AdjustableRateLimiter.create(config.getRate().getMaxRate());
        // concurrent limiter
        this.semaphore = new AdjustableSemaphore(config.getConcurrent().getMaxPermit(), true);
        // counter limiter
        // request limiter
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        cacheBuilder.expireAfterWrite(config.getCounter().getTimeout(), TimeUnit.MILLISECONDS);
        this.counter = cacheBuilder.build(CacheLoader.from(() -> new AtomicLong(0)));

        // the refresh rateLimiter
        rateLimiter.setRate(config.getRate().getRateUnit());
        // the refresh semaphore
        semaphore.setMaxPermits(config.getConcurrent().getMaxPermit());
        return true;
    }

    @Override
    protected Acquire tryAcquireConcurrent() {
        LimiterConfig.ConcurrentLimiterConfig concurrentConfig = config.getConcurrent();
        try {
            if (concurrentConfig.getTimeout() > 0) {
                // try acquire by timeout
                return semaphore.tryAcquire(concurrentConfig.getPermitUnit(), concurrentConfig.getTimeout(),
                        TimeUnit.MILLISECONDS) ? Acquire.SUCCESS : Acquire.FAILURE;
            }

            // try acquire
            return semaphore.tryAcquire(concurrentConfig.getPermitUnit()) ? Acquire.SUCCESS : Acquire.FAILURE;
        } catch (Exception e) {
            log.error("Try acquire local concurrent exception", e);
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
            log.error("Try acquire local rate limiter exception", e);
            return Acquire.EXCEPTION;
        }
    }

    @Override
    protected Acquire tryAcquireCounter() {
        LimiterConfig.CounterLimiterConfig counterConfig = config.getCounter();

        try {
            // get the current time window
            long currentSeconds = System.currentTimeMillis() / counterConfig.getCountUnit();
            if (counter.get(currentSeconds).incrementAndGet() > counterConfig.getMaxCount()) {
                return Acquire.FAILURE;
            }

            return Acquire.SUCCESS;
        } catch (Exception e) {
            log.error("Try acquire local counter limiter exception", e);
            return Acquire.EXCEPTION;
        }
    }

}
