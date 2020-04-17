package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.*;
import cn.neural.common.utils.CloneUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The Abstract Call Limiter.
 *
 * @author lry
 * @apiNote The main implementation of original call limiting
 */
@Slf4j
@Getter
public abstract class AbstractCallLimiter implements ILimiter {

    protected volatile LimiterConfig config = null;
    protected volatile LimiterStatistics statistics = new LimiterStatistics();

    @Override
    public boolean refresh(LimiterConfig limiterConfig) throws Exception {
        log.info("Refresh the current limit configuration information: {}", limiterConfig);
        if (null == limiterConfig || this.config.equals(limiterConfig)) {
            return true;
        }
        if (limiterConfig.getConcurrent().getPermitUnit() < 1 ||
                limiterConfig.getConcurrent().getMaxPermit() >= limiterConfig.getConcurrent().getPermitUnit()) {
            return false;
        }

        this.config = CloneUtils.clone(limiterConfig);
        return true;
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
                        decrementConcurrent();
                    }
                case EXCEPTION:
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
        // the total exceed of statistical traffic
        statistics.exceedTraffic(eventType);

        // print exceed log
        log.warn("The {} exceed, [{}]-[{}]", eventType, config, statistics);

        // the broadcast event of traffic exceed
        //EventCollect.onEvent(eventType, limiterConfig, statistics.getStatisticsData());

        // the execute strategy with traffic exceed
        if (null != config.getStrategy()) {
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
        }

        // the wrapper of original call
        return statistics.wrapperOriginalCall(limiterContext, originalCall);
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
