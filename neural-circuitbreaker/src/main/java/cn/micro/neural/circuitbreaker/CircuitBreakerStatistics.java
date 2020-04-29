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
import java.util.concurrent.atomic.LongAdder;

/**
 * CircuitBreakerStatistics
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

    // === request/success/failure/timeout/rejection


    /**
     * The wrapper of original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    public Object wrapperOriginalCall(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        try {
            try {
                // Step 1: increment traffic
                requestCounter.increment();
            } catch (Exception e) {
                log.error("Total increment traffic exception", e);
            }

            // original call
            Object result = originalCall.call(originalContext);

            try {
                // Step 2: success traffic
                successCounter.increment();
            } catch (Exception e) {
                log.error("Total success traffic exception", e);
            }
            return result;
        } catch (Throwable t) {
            try {
                // Step 3: exception traffic
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
        return map;
    }

}
