package org.micro.neural.limiter.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.micro.neural.OriginalCall;
import org.micro.neural.config.event.EventCollect;
import org.micro.neural.limiter.LimiterExceedException;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.limiter.LimiterGlobalConfig;

/**
 * The Abstract Call Limiter.
 *
 * @author lry
 * @apiNote The main implementation of original call limiting
 */
@Slf4j
public abstract class AbstractCallLimiter extends AbstractCheckLimiter {

    @Override
    public Object doOriginalCall(OriginalCall originalCall) throws Throwable {
        if (super.checkDisable()) {
            // the don't need limiting
            return originalCall.call();
        }

        // the total request of statistical traffic
        statistics.totalRequestTraffic();

        // the concurrent limiter and original call
        return doConcurrencyOriginalCall(originalCall);
    }

    /**
     * The concurrent limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doConcurrencyOriginalCall(OriginalCall originalCall) throws Throwable {
        // the check concurrent limiting exceed
        if (super.checkConcurrencyExceed()) {
            // try acquire concurrent
            switch (incrementConcurrent()) {
                case FAILURE:
                    // the concurrent exceed
                    return doStrategyProcess(LimiterGlobalConfig.EventType.CONCURRENT_EXCEED, originalCall);
                case SUCCESS:
                    // the concurrent success must be released
                    try {
                        return doRateOriginalCall(originalCall);
                    } finally {
                        decrementConcurrent();
                    }
                case EXCEPTION:
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check ConcurrencyLimiter or exception or other
        return doRateOriginalCall(originalCall);
    }

    /**
     * The rate limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doRateOriginalCall(OriginalCall originalCall) throws Throwable {
        // the check rate limiting exceed
        if (super.checkRateExceed()) {
            switch (tryAcquireRate()) {
                case FAILURE:
                    // the rate exceed
                    return doStrategyProcess(LimiterGlobalConfig.EventType.RATE_EXCEED, originalCall);
                case SUCCESS:
                    // the pass success case
                case EXCEPTION:
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check RateLimiter or success or exception or other
        return statistics.wrapperOriginalCall(originalCall);
    }

    /**
     * The execute strategy process of limiting exceed
     *
     * @param eventType    The event type
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doStrategyProcess(LimiterGlobalConfig.EventType eventType,
                                     OriginalCall originalCall) throws Throwable {
        // the total exceed of statistical traffic
        statistics.exceedTraffic(eventType);

        // print exceed log
        log.warn("The {} exceed, [{}]-[{}]", eventType, limiterConfig, statistics);

        // the broadcast event of traffic exceed
        EventCollect.onEvent(eventType, limiterConfig, statistics.getStatisticsData());

        // the execute strategy with traffic exceed
        if (null != limiterConfig.getStrategy()) {
            switch (limiterConfig.getStrategy()) {
                case FALLBACK:
                    return originalCall.fallback();
                case EXCEPTION:
                    throw new LimiterExceedException(eventType.name());
                case NON:
                    // the skip non case
                default:
                    // the skip other case
            }
        }

        // the wrapper of original call
        return statistics.wrapperOriginalCall(originalCall);
    }

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
        SUCCESS(0),

        /**
         * The failure of limiter
         */
        FAILURE(1),

        /**
         * The exception of limiter
         */
        EXCEPTION(2);

        private int value;

    }

}
