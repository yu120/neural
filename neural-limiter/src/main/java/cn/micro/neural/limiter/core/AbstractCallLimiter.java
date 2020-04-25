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
            // check request limiter config
            LimiterConfig.RequestLimiterConfig request = limiterConfig.getRequest();
            if (request.getRequestUnit() < 1 || request.getMaxRequest() < 1
                    || request.getMaxRequest() <= request.getRequestUnit()) {
                log.warn("Illegal request limiter config: {}", limiterConfig);
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
            return originalCall.call();
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
        // the check concurrent limiting exceed
        if (LimiterConfig.Switch.ON == config.getConcurrent().getEnable()) {
            // try acquire concurrent
            switch (incrementConcurrent()) {
                case FAILURE:
                    // the concurrent exceed
                    return doStrategyProcess(limiterContext, EventType.CONCURRENT_EXCEED, originalCall);
                case SUCCESS:
                    // the concurrent success must be released
                    try {
                        return doRateOriginalCall(limiterContext, originalCall);
                    } finally {
                        // only need to be released after success
                        decrementConcurrent();
                    }
                case EXCEPTION:
                    this.collectEvent(EventType.CONCURRENT_EXCEPTION);
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check ConcurrentLimiter or exception or other
        return doRateOriginalCall(limiterContext, originalCall);
    }

    /**
     * The rate limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doRateOriginalCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        // the check rate limiting exceed
        if (LimiterConfig.Switch.ON == config.getRate().getEnable()) {
            switch (tryAcquireRate()) {
                case FAILURE:
                    // the rate exceed
                    return doStrategyProcess(limiterContext, EventType.RATE_EXCEED, originalCall);
                case SUCCESS:
                    // the pass success case
                case EXCEPTION:
                    this.collectEvent(EventType.RATE_EXCEPTION);
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check RateLimiter or success or exception or other
        return doRequestOriginalCall(limiterContext, originalCall);
    }

    /**
     * The request limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doRequestOriginalCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        // the check request limiting exceed
        if (LimiterConfig.Switch.ON == config.getRate().getEnable()) {
            switch (tryAcquireRequest()) {
                case FAILURE:
                    // the request exceed
                    return doStrategyProcess(limiterContext, EventType.REQUEST_EXCEED, originalCall);
                case SUCCESS:
                    // the pass success case
                case EXCEPTION:
                    this.collectEvent(EventType.REQUEST_EXCEPTION);
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check RateLimiter or success or exception or other
        return statistics.wrapperOriginalCall(limiterContext, originalCall);
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
                return originalCall.fallback();
            case EXCEPTION:
                throw new LimiterExceedException(eventType.name());
            case NON:
                // the skip non case
            default:
                // the skip other case
        }

        // the wrapper of original call
        return statistics.wrapperOriginalCall(limiterContext, originalCall);
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
     * The increment of concurrent limiter.
     *
     * @return The excess of limiting
     */
    protected abstract Acquire incrementConcurrent();

    /**
     * The decrement of concurrent limiter.
     */
    protected abstract void decrementConcurrent();

    /**
     * The acquire of rate limiter.
     *
     * @return The excess of limiting
     */
    protected abstract Acquire tryAcquireRate();

    /**
     * The acquire windows time of request limiter.
     *
     * @return The excess of limiting
     */
    protected abstract Acquire tryAcquireRequest();

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
         * The non rule of limiter
         */
        NON_RULE(2),

        /**
         * The failure of limiter
         */
        FAILURE(0),

        /**
         * The exception of limiter
         */
        EXCEPTION(-1);

        private int value;

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
