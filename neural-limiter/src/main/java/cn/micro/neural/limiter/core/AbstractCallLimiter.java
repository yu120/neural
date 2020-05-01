package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterStatistics;
import cn.micro.neural.limiter.event.EventListener;
import cn.micro.neural.limiter.event.EventType;
import cn.micro.neural.limiter.exception.LimiterException;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import cn.neural.common.utils.BeanUtils;
import cn.neural.common.utils.CloneUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
    public synchronized boolean refresh(LimiterConfig config) throws Exception {
        try {
            log.info("Refresh the current limiter config: {}", config);
            if (null == config || config.equals(this.config)) {
                return false;
            }

            // check concurrent limiter config
            LimiterConfig.ConcurrentLimiterConfig concurrent = config.getConcurrent();
            if (concurrent.getPermitUnit() < 1 || concurrent.getMaxPermit() < 1
                    || concurrent.getMaxPermit() <= concurrent.getPermitUnit()) {
                log.warn("Illegal concurrent limiter config: {}", config);
                return false;
            }
            // check rate limiter config
            LimiterConfig.RateLimiterConfig rate = config.getRate();
            if (rate.getRateUnit() < 1 || rate.getMaxRate() < 1
                    || rate.getMaxRate() <= rate.getRateUnit()) {
                log.warn("Illegal rate limiter config: {}", config);
                return false;
            }
            // check counter limiter config
            LimiterConfig.CounterLimiterConfig counter = config.getCounter();
            if (counter.getCountUnit() < 1 || counter.getMaxCount() < 1
                    || counter.getMaxCount() <= counter.getCountUnit()) {
                log.warn("Illegal counter limiter config: {}", config);
                return false;
            }

            // Copy properties attributes after deep copy
            BeanUtils.copyProperties(CloneUtils.clone(config), this.config);

            return tryRefresh(config);
        } catch (Exception e) {
            this.collectEvent(EventType.REFRESH_EXCEPTION, config);
            throw e;
        }
    }

    @Override
    public Object wrapperCall(final OriginalContext originalContext, final OriginalCall originalCall) throws Throwable {
        // the don't need limiting
        if (null == config || LimiterConfig.Switch.OFF == config.getEnable()) {
            return originalCall.call(originalContext);
        }

        // the concurrent limiter and original call
        return doConcurrentOriginalCall(originalContext, originalCall);
    }

    /**
     * The concurrent limiter and original call
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doConcurrentOriginalCall(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        // if the concurrent limiter switch is closed, then continue to execute
        if (LimiterConfig.Switch.OFF == config.getConcurrent().getEnable()) {
            return doRateOriginalCall(originalContext, originalCall);
        }

        // try acquire concurrent
        switch (tryAcquireConcurrent()) {
            case FAILURE:
                // try acquire concurrent exceed
                this.collectEvent(EventType.CONCURRENT_EXCEED);
                return statistics.doStrategyProcess(originalContext, EventType.CONCURRENT_EXCEED,
                        config.getConcurrent().getStrategy(), originalCall);
            case SUCCESS:
                // try acquire concurrent success
                try {
                    return doRateOriginalCall(originalContext, originalCall);
                } finally {
                    // only need to be released after success
                    releaseConcurrent();
                }
            case EXCEPTION:
                // try acquire concurrent exceptions
                this.collectEvent(EventType.CONCURRENT_EXCEPTION);
                return doRateOriginalCall(originalContext, originalCall);
            default:
                // illegal concurrent strategy type
                throw new LimiterException("Illegal concurrent strategy type");
        }
    }

    /**
     * The rate limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doRateOriginalCall(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        // if the rate limiter switch is closed, then continue to execute
        if (LimiterConfig.Switch.OFF == config.getRate().getEnable()) {
            return doCounterOriginalCall(originalContext, originalCall);
        }

        // try acquire rate
        switch (tryAcquireRate()) {
            case FAILURE:
                // try acquire rate exceed
                this.collectEvent(EventType.RATE_EXCEED);
                return statistics.doStrategyProcess(originalContext, EventType.RATE_EXCEED,
                        config.getConcurrent().getStrategy(), originalCall);
            case SUCCESS:
                // try acquire rate success
                return doCounterOriginalCall(originalContext, originalCall);
            case EXCEPTION:
                // try acquire rate exception
                this.collectEvent(EventType.RATE_EXCEPTION);
                return doCounterOriginalCall(originalContext, originalCall);
            default:
                // illegal rate strategy type
                throw new LimiterException("Illegal rate strategy type");
        }
    }

    /**
     * The counter limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doCounterOriginalCall(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        // if the counter limiter switch is closed, then continue to execute
        if (LimiterConfig.Switch.OFF == config.getCounter().getEnable()) {
            return statistics.wrapperOriginalCall(originalContext, originalCall);
        }

        // try acquire counter
        switch (tryAcquireCounter()) {
            case FAILURE:
                // try acquire counter exceed
                this.collectEvent(EventType.COUNTER_EXCEED);
                return statistics.doStrategyProcess(originalContext, EventType.COUNTER_EXCEED,
                        config.getConcurrent().getStrategy(), originalCall);
            case SUCCESS:
                // try acquire counter success
                return statistics.wrapperOriginalCall(originalContext, originalCall);
            case EXCEPTION:
                // try acquire counter exception
                this.collectEvent(EventType.COUNTER_EXCEPTION);
                return statistics.wrapperOriginalCall(originalContext, originalCall);
            default:
                // illegal counter strategy type
                throw new LimiterException("Illegal counter strategy type");
        }
    }

    @Override
    public Map<String, Long> collect() {
        try {
            return statistics.collectThenReset();
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
     * @param config configuration
     * @return true is success
     */
    protected abstract boolean tryRefresh(LimiterConfig config);

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
