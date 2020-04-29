package cn.micro.neural.circuitbreaker;

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
 * The statistics of Limiter.
 *
 * @author lry
 */
@Data
@Slf4j
public class CircuitBreakerStatistics implements Serializable {

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

    // === elapsed/maxElapsed/concurrent/maxConcurrent/concurrentExceed/rateExceed

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

    /**
     * The wrapper of original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    public Object wrapperOriginalCall(final OriginalContext originalContext, final OriginalCall originalCall) throws Throwable {
        final long startTime = System.currentTimeMillis();

        try {
            // Step 1: increment traffic
            requestCounter.increment();
            maxConcurrentAccumulator.accumulate(concurrentCounter.incrementAndGet());

            // original call
            Object result = originalCall.call(originalContext);

            // Step 2: success traffic
            successCounter.increment();
            return result;
        } catch (Throwable t) {
            // Step 3: exception traffic
            try {
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
                // Step 4: decrement traffic
                long elapsed = System.currentTimeMillis() - startTime;
                totalElapsedAccumulator.accumulate(elapsed);
                maxElapsedAccumulator.accumulate(elapsed);
            } catch (Exception e) {
                log.error("Total decrement traffic exception", e);
            }
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
        long failure = failureCounter.sumThenReset();
        long timeout = timeoutCounter.sumThenReset();
        long rejected = rejectedCounter.sumThenReset();
        long fallback = fallbackCounter.sumThenReset();

        long avgElapsed = success < 1 ? 0 : (totalElapsedAccumulator.getThenReset() / success);
        long maxElapsed = maxElapsedAccumulator.getThenReset();
        long concurrent = concurrentCounter.get();
        long maxConcurrent = maxConcurrentAccumulator.getThenReset();
        if (request < 1) {
            return map;
        }

        // statistics number
        map.put(SUCCESS_KEY, success);
        map.put(REQUEST_KEY, request);
        map.put(FAILURE_KEY, failure);
        map.put(TIMEOUT_KEY, timeout);
        map.put(REJECTED_KEY, rejected);
        map.put(FALLBACK_KEY, fallback);

        map.put(AVG_ELAPSED_KEY, avgElapsed);
        map.put(MAX_ELAPSED_KEY, maxElapsed);
        map.put(CONCURRENT_KEY, concurrent);
        map.put(MAX_CONCURRENT_KEY, maxConcurrent);
        return map;
    }

    /**
     * Statistical data
     *
     * @return statistics data map
     */
    public Map<String, Long> getStatisticsData() {
        final Map<String, Long> map = new LinkedHashMap<>();
        long success = successCounter.sum();
        map.put(SUCCESS_KEY, success);
        map.put(REQUEST_KEY, requestCounter.sum());
        map.put(FAILURE_KEY, failureCounter.sum());
        map.put(TIMEOUT_KEY, timeoutCounter.sum());
        map.put(REJECTED_KEY, rejectedCounter.sum());
        map.put(FALLBACK_KEY, fallbackCounter.sum());

        map.put(AVG_ELAPSED_KEY, success < 1 ? 0 : (totalElapsedAccumulator.get() / success));
        map.put(MAX_ELAPSED_KEY, maxElapsedAccumulator.get());
        map.put(CONCURRENT_KEY, concurrentCounter.get());
        map.put(MAX_CONCURRENT_KEY, maxConcurrentAccumulator.get());
        return map;
    }

}
