package cn.micro.neural.limiter;

import cn.micro.neural.limiter.event.EventType;
import cn.micro.neural.limiter.exception.LimiterExceedException;
import cn.micro.neural.limiter.exception.LimiterException;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * LimiterStatistics
 *
 * @author lry
 */
@Data
@Slf4j
public class LimiterStatistics implements Serializable {

    private static final long serialVersionUID = -7032404135074863381L;

    // === request/success/failure/timeout/rejection

    /**
     * The total request counter in the current time window: Calculation QPS
     */
    private final LongAdder requestCounter = new LongAdder();
    /**
     * The total success counter in the current time window: Calculation TPS
     */
    private final LongAdder successCounter = new LongAdder();
    /**
     * The total failure counter in the current time window
     */
    private final LongAdder failureCounter = new LongAdder();
    /**
     * The total exception counter in the current time window
     */
    private final LongAdder timeoutCounter = new LongAdder();
    /**
     * The total exception counter in the current time window
     */
    private final LongAdder rejectedCounter = new LongAdder();
    /**
     * The total fallback counter in the current time window
     */
    private final LongAdder fallbackCounter = new LongAdder();


    // === avgElapsed/maxElapsed/concurrent/maxConcurrent

    /**
     * The total elapsed counter in the current time window
     */
    private final LongAccumulator totalElapsedAccumulator = new LongAccumulator(Long::sum, 0);
    /**
     * The max elapsed counter in the current time window
     */
    private final LongAccumulator maxElapsedAccumulator = new LongAccumulator(Long::max, 0);
    /**
     * The total concurrent exceed counter in the current time window
     */
    private final AtomicLong concurrentCounter = new AtomicLong(0);
    /**
     * The max concurrent counter in the current time window
     */
    private final LongAccumulator maxConcurrentAccumulator = new LongAccumulator(Long::max, 0);

    // === rateExceed/counterExceed/concurrentExceed

    /**
     * The total rate exceed counter in the current time window
     */
    private final LongAdder rateExceedCounter = new LongAdder();
    /**
     * The total counter exceed counter in the current time window
     */
    private final LongAdder counterExceedCounter = new LongAdder();
    /**
     * The total concurrent exceed counter in the current time window
     */
    private final LongAdder concurrentExceedCounter = new LongAdder();

    /**
     * The wrapper of original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    public Object wrapperOriginalCall(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        final long startTime = System.currentTimeMillis();

        try {
            try {
                // Step 1: increment traffic
                requestCounter.increment();
                maxConcurrentAccumulator.accumulate(concurrentCounter.incrementAndGet());
            } catch (Exception e) {
                log.error("Total increment traffic exception", e);
            }

            // Step 2: original call
            Object result = originalCall.call(originalContext);

            try {
                // Step 3: success traffic
                successCounter.increment();
            } catch (Exception e) {
                log.error("Total success traffic exception", e);
            }

            return result;
        } catch (Throwable t) {
            try {
                // Step 4: exception traffic
                failureCounter.increment();
                if (t instanceof TimeoutException) {
                    timeoutCounter.increment();
                } else if (t instanceof RejectedExecutionException) {
                    rejectedCounter.increment();
                }
            } catch (Exception e) {
                log.error("Total exception traffic exception", e);
            }

            throw t;
        } finally {
            try {
                // Step 5: decrement traffic
                long elapsed = System.currentTimeMillis() - startTime;
                totalElapsedAccumulator.accumulate(elapsed);
                maxElapsedAccumulator.accumulate(elapsed);
            } catch (Exception e) {
                log.error("Total decrement traffic exception", e);
            }
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
    public Object doStrategyProcess(OriginalContext originalContext, EventType eventType,
                                    LimiterConfig.Strategy strategy, OriginalCall originalCall) throws Throwable {
        log.warn("The limiter exceed[{}]", eventType);

        // the total exceed of statistical traffic
        switch (eventType) {
            case RATE_EXCEED:
                rateExceedCounter.increment();
                break;
            case COUNTER_EXCEED:
                counterExceedCounter.increment();
                break;
            case CONCURRENT_EXCEED:
                concurrentExceedCounter.increment();
                break;
            default:
                log.warn("Illegal event type: {}", eventType);
        }

        // the execute strategy with traffic exceed
        switch (strategy) {
            case FALLBACK:
                fallbackCounter.increment();
                return originalCall.fallback(originalContext);
            case EXCEPTION:
                throw new LimiterExceedException(eventType.name());
            case IGNORE:
                return wrapperOriginalCall(originalContext, originalCall);
            default:
                throw new LimiterException("Illegal strategy type");
        }
    }

    public static final String REQUEST_KEY = "request";
    public static final String SUCCESS_KEY = "success";
    public static final String FAILURE_KEY = "failure";
    public static final String TIMEOUT_KEY = "timeout";
    public static final String REJECTED_KEY = "rejected";
    public static final String FALLBACK_KEY = "fallback";

    public static final String AVG_ELAPSED_KEY = "avg_elapsed";
    public static final String MAX_ELAPSED_KEY = "max_elapsed";
    public static final String CONCURRENT_KEY = "concurrent";
    public static final String MAX_CONCURRENT_KEY = "max_concurrent";

    public static final String RATE_EXCEED_KEY = "rate_exceed";
    public static final String COUNTER_EXCEED_KEY = "counter_exceed";
    public static final String CONCURRENT_EXCEED_KEY = "concurrent_exceed";

    /**
     * Collect then reset statistics
     *
     * @return statistics data map
     */
    public Map<String, Long> collectThenReset() {
        final Map<String, Long> map = new LinkedHashMap<>();
        // reset number
        long success = successCounter.sumThenReset();
        long request = requestCounter.sumThenReset();
        if (request < 1) {
            return map;
        }

        // statistics number
        map.put(SUCCESS_KEY, success);
        map.put(REQUEST_KEY, request);
        map.put(FAILURE_KEY, failureCounter.sumThenReset());
        map.put(TIMEOUT_KEY, timeoutCounter.sumThenReset());
        map.put(REJECTED_KEY, rejectedCounter.sumThenReset());
        map.put(FALLBACK_KEY, fallbackCounter.sumThenReset());

        map.put(AVG_ELAPSED_KEY, success < 1 ? 0 : (totalElapsedAccumulator.getThenReset() / success));
        map.put(MAX_ELAPSED_KEY, maxElapsedAccumulator.getThenReset());
        map.put(CONCURRENT_KEY, concurrentCounter.get());
        map.put(MAX_CONCURRENT_KEY, maxConcurrentAccumulator.getThenReset());

        map.put(RATE_EXCEED_KEY, rateExceedCounter.sumThenReset());
        map.put(COUNTER_EXCEED_KEY, counterExceedCounter.sumThenReset());
        map.put(CONCURRENT_EXCEED_KEY, concurrentExceedCounter.sumThenReset());
        return map;
    }

}
