package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.*;
import cn.micro.neural.limiter.EventListener;
import cn.neural.common.utils.BeanUtils;
import cn.neural.common.utils.CloneUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * The Abstract Call Limiter.
 *
 * @author lry
 * @apiNote The main implementation of original call limiting
 */
@Slf4j
@Getter
public abstract class AbstractCallLimiter implements ILimiter {

    private final Set<EventListener> listeners = new LinkedHashSet<>();
    protected volatile LimiterConfig config = new LimiterConfig();
    protected volatile LimiterStatistics statistics = new LimiterStatistics();

    @Override
    public void addListener(EventListener... eventListeners) {
        Collections.addAll(listeners, eventListeners);
    }

    @Override
    public synchronized boolean refresh(LimiterConfig limiterConfig) throws Exception {
        try {
            log.info("Refresh the current limiter config: {}", limiterConfig);
            if (null == limiterConfig || this.config.equals(limiterConfig)) {
                return false;
            }

            // check concurrent limiter config
            LimiterConfig.ConcurrentLimiterConfig concurrent = limiterConfig.getConcurrent();
            if (concurrent.getPermitUnit() < 1 || concurrent.getMaxPermit() < 1
                    || concurrent.getMaxPermit() <= concurrent.getPermitUnit()) {
                log.warn("Illegal concurrent limiter config: {}", limiterConfig);
                return false;
            }
            // check rate limiter config
            LimiterConfig.RateLimiterConfig rate = limiterConfig.getRate();
            if (rate.getRateUnit() < 1 || rate.getMaxRate() < 1
                    || rate.getMaxRate() <= rate.getRateUnit()) {
                log.warn("Illegal rate limiter config: {}", limiterConfig);
                return false;
            }
            // check counter limiter config
            LimiterConfig.CounterLimiterConfig counter = limiterConfig.getCounter();
            if (counter.getCountUnit() < 1 || counter.getMaxCount() < 1
                    || counter.getMaxCount() <= counter.getCountUnit()) {
                log.warn("Illegal counter limiter config: {}", limiterConfig);
                return false;
            }

            // Copy properties attributes after deep copy
            BeanUtils.copyProperties(CloneUtils.clone(limiterConfig), this.config);

            return tryRefresh(limiterConfig);
        } catch (Exception e) {
            this.collectEvent(EventType.REFRESH_EXCEPTION);
            throw e;
        }
    }

    @Override
    public Object wrapperCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        // the don't need limiting
        if (null == config || LimiterConfig.Switch.OFF == config.getEnable()) {
            return originalCall.call(limiterContext);
        }

        // the concurrent limiter and original call
        return doConcurrentOriginalCall(limiterContext, originalCall);
    }

    /**
     * The concurrent limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doConcurrentOriginalCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        // if the concurrent limiter switch is closed, then continue to execute
        if (LimiterConfig.Switch.OFF == config.getConcurrent().getEnable()) {
            return doRateOriginalCall(limiterContext, originalCall);
        }

        // try acquire concurrent
        switch (tryAcquireConcurrent()) {
            case FAILURE:
                // try acquire concurrent exceed
                return doStrategyProcess(limiterContext, EventType.CONCURRENT_EXCEED, originalCall);
            case SUCCESS:
                // try acquire concurrent success
                try {
                    return doRateOriginalCall(limiterContext, originalCall);
                } finally {
                    // only need to be released after success
                    releaseConcurrent();
                }
            case EXCEPTION:
                // try acquire concurrent exceptions
                this.collectEvent(EventType.CONCURRENT_EXCEPTION);
                return doRateOriginalCall(limiterContext, originalCall);
            default:
                // illegal concurrent strategy type
                throw new IllegalArgumentException("Illegal concurrent strategy type");
        }
    }

    /**
     * The rate limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doRateOriginalCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        // if the rate limiter switch is closed, then continue to execute
        if (LimiterConfig.Switch.OFF == config.getRate().getEnable()) {
            return doCounterOriginalCall(limiterContext, originalCall);
        }

        // try acquire rate
        switch (tryAcquireRate()) {
            case FAILURE:
                // try acquire rate exceed
                return doStrategyProcess(limiterContext, EventType.RATE_EXCEED, originalCall);
            case SUCCESS:
                // try acquire rate success
                return doCounterOriginalCall(limiterContext, originalCall);
            case EXCEPTION:
                // try acquire rate exception
                this.collectEvent(EventType.RATE_EXCEPTION);
                return doCounterOriginalCall(limiterContext, originalCall);
            default:
                // illegal rate strategy type
                throw new IllegalArgumentException("Illegal rate strategy type");
        }
    }

    /**
     * The counter limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doCounterOriginalCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        // if the counter limiter switch is closed, then continue to execute
        if (LimiterConfig.Switch.OFF == config.getCounter().getEnable()) {
            return statistics.wrapperOriginalCall(limiterContext, originalCall);
        }

        // try acquire counter
        switch (tryAcquireCounter()) {
            case FAILURE:
                // try acquire counter exceed
                return doStrategyProcess(limiterContext, EventType.COUNTER_EXCEED, originalCall);
            case SUCCESS:
                // try acquire counter success
                return statistics.wrapperOriginalCall(limiterContext, originalCall);
            case EXCEPTION:
                // try acquire counter exception
                this.collectEvent(EventType.COUNTER_EXCEPTION);
                return statistics.wrapperOriginalCall(limiterContext, originalCall);
            default:
                // illegal counter strategy type
                throw new IllegalArgumentException("Illegal counter strategy type");
        }
    }

    /**
     * The execute strategy process of limiting exceed
     *
     * @param eventType    The event type
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doStrategyProcess(LimiterContext limiterContext, EventType eventType, OriginalCall originalCall) throws Throwable {
        // print exceed log
        log.warn("The limiter exceed[{}]", eventType);

        // the total exceed of statistical traffic
        statistics.exceedTraffic(eventType);
        // the broadcast event of traffic exceed
        this.collectEvent(eventType, statistics.getStatisticsData());

        // the execute strategy with traffic exceed
        switch (config.getStrategy()) {
            case FALLBACK:
                // fallback
                return originalCall.fallback(limiterContext);
            case EXCEPTION:
                // throw exception
                throw new LimiterExceedException(eventType.name());
            case IGNORE:
                // ignore
                return statistics.wrapperOriginalCall(limiterContext, originalCall);
            default:
                throw new IllegalArgumentException("Illegal strategy type");
        }
    }

    @Override
    public Map<String, Long> statistics() {
        try {
            return statistics.getStatisticsData();
        } catch (Exception e) {
            this.collectEvent(EventType.STATISTICS_EXCEPTION);
            log.error("The limiter[{}] statistics exception", config.identity(), e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> collect() {
        try {
            return statistics.getAndReset();
        } catch (Exception e) {
            this.collectEvent(EventType.COLLECT_EXCEPTION);
            log.error("The limiter[{}] collect exception", config.identity(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * The collect event
     *
     * @param eventType {@link EventType}
     * @param args      attachment parameters
     */
    private void collectEvent(EventType eventType, Object... args) {
        for (EventListener eventListener : listeners) {
            try {
                eventListener.onEvent(config, eventType, args);
            } catch (Exception e) {
                log.error("The collect event exception", e);
            }
        }
    }

    /**
     * The do refresh
     *
     * @param limiterConfig {@link LimiterConfig}
     * @return true is success
     */
    protected abstract boolean tryRefresh(LimiterConfig limiterConfig);

    /**
     * The try acquire and increment of concurrent limiter.
     *
     * @return {@link Acquire}
     */
    protected abstract Acquire tryAcquireConcurrent();

    /**
     * The release(decrement) of concurrent limiter.
     */
    protected abstract void releaseConcurrent();

    /**
     * The try acquire of rate limiter.
     *
     * @return {@link Acquire}
     */
    protected abstract Acquire tryAcquireRate();

    /**
     * The acquire windows time of counter limiter.
     *
     * @return {@link Acquire}
     */
    protected abstract Acquire tryAcquireCounter();

    /**
     * The Excess of Limiter.
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Acquire {

        /**
         * The success of limiter
         */
        SUCCESS(1),
        /**
         * The failure of limiter
         */
        FAILURE(0),
        /**
         * The exception of limiter
         */
        EXCEPTION(-1);

        private final int value;

        public static Acquire valueOf(int value) {
            for (Acquire e : values()) {
                if (e.getValue() == value) {
                    return e;
                }
            }

            return Acquire.EXCEPTION;
        }

    }

}
