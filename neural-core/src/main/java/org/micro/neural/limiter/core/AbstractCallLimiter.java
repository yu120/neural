package org.micro.neural.limiter.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.micro.neural.NeuralContext;
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
    public Object wrapperCall(NeuralContext neuralContext, OriginalCall originalCall) throws Throwable {
        if (super.checkDisable()) {
            // the don't need limiting
            return statistics.wrapperOriginalCall(neuralContext, originalCall);
        }

        // the concurrent limiter and original call
        return doConcurrentOriginalCall(neuralContext, originalCall);
    }

    /**
     * The concurrent limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doConcurrentOriginalCall(NeuralContext neuralContext, OriginalCall originalCall) throws Throwable {
        // the check concurrent limiting exceed
        if (super.checkConcurrentEnable()) {
            // try acquire concurrent
            switch (incrementConcurrent()) {
                case FAILURE:
                    // the concurrent exceed
                    return doStrategyProcess(neuralContext, LimiterGlobalConfig.EventType.CONCURRENT_EXCEED, originalCall);
                case SUCCESS:
                    // the concurrent success must be released
                    try {
                        return doRateOriginalCall(neuralContext, originalCall);
                    } finally {
                        decrementConcurrent();
                    }
                case EXCEPTION:
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check ConcurrentLimiter or exception or other
        return doRateOriginalCall(neuralContext, originalCall);
    }

    /**
     * The rate limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doRateOriginalCall(NeuralContext neuralContext, OriginalCall originalCall) throws Throwable {
        // the check rate limiting exceed
        if (super.checkRateEnable()) {
            switch (tryAcquireRate()) {
                case FAILURE:
                    // the rate exceed
                    return doStrategyProcess(neuralContext, LimiterGlobalConfig.EventType.RATE_EXCEED, originalCall);
                case SUCCESS:
                    // the pass success case
                case EXCEPTION:
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check RateLimiter or success or exception or other
        return doRequestOriginalCall(neuralContext, originalCall);
    }

    /**
     * The request limiter and original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doRequestOriginalCall(NeuralContext neuralContext, OriginalCall originalCall) throws Throwable {
        // the check request limiting exceed
        if (super.checkRequestEnable()) {
            switch (tryAcquireRequest()) {
                case FAILURE:
                    // the request exceed
                    return doStrategyProcess(neuralContext, LimiterGlobalConfig.EventType.REQUEST_EXCEED, originalCall);
                case SUCCESS:
                    // the pass success case
                case EXCEPTION:
                    // the skip exception case
                default:
                    // the skip other case
            }
        }

        // the skip non check RateLimiter or success or exception or other
        return statistics.wrapperOriginalCall(neuralContext, originalCall);
    }

    /**
     * The execute strategy process of limiting exceed
     *
     * @param eventType    The event type
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    private Object doStrategyProcess(NeuralContext neuralContext, LimiterGlobalConfig.EventType eventType,
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
        return statistics.wrapperOriginalCall(neuralContext, originalCall);
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
