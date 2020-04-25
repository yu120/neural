package cn.micro.neural.limiter;

import cn.micro.neural.limiter.event.EventType;
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
public class LimiterStatistics implements Serializable {

    private static final long serialVersionUID = -7032404135074863381L;

    // ==== common statistics

    public static final String SUCCESS_KEY = "success";
    public static final String REQUEST_KEY = "request";
    public static final String FAILURE_KEY = "failure";
    public static final String AVG_ELAPSED_KEY = "avg_elapsed";
    public static final String MAX_ELAPSED_KEY = "max_elapsed";
    public static final String CONCURRENT_KEY = "concurrent";
    public static final String MAX_CONCURRENT_KEY = "max_concurrent";

    // ==== limiter statistics

    public static final String CONCURRENT_EXCEED_KEY = "concurrent_exceed";
    public static final String RATE_EXCEED_KEY = "rate_exceed";

    // ==== exception statistics

    public static final String TIMEOUT_TIMES_KEY = "timeout";
    public static final String REJECTED_TIMES_KEY = "rejected";

    /**
     * The total concurrent exceed counter in the current time window
     */
    private final LongAdder concurrentExceedCounter = new LongAdder();
    /**
     * The total rate exceed counter in the current time window
     */
    private final LongAdder rateExceedCounter = new LongAdder();

    /**
     * The total exceed of statistical traffic
     */
    public void exceedTraffic(EventType eventType) {
        try {
            switch (eventType) {
                case CONCURRENT_EXCEED:
                    // increment exceed times
                    concurrentExceedCounter.increment();
                    return;
                case RATE_EXCEED:
                    // increment exceed times
                    rateExceedCounter.increment();
                    return;
                default:
                    log.error("The illegal EventType: {}", eventType);
            }
        } catch (Exception e) {
            log.error("The total request traffic is exception", e);
        }
    }

    // === request/success/failure/timeout/rejection

    /**
     * The total success counter in the current time window: Calculation TPS
     */
    private final LongAdder successCounter = new LongAdder();
    /**
     * The total request counter in the current time window: Calculation QPS
     */
    private final LongAdder requestCounter = new LongAdder();
    /**
     * The total failure counter in the current time window
     */
    private final LongAdder failureCounter = new LongAdder();

    // === elapsed/maxElapsed

    /**
     * The total elapsed counter in the current time window
     */
    private final LongAccumulator totalElapsedAccumulator = new LongAccumulator(Long::sum, 0);
    /**
     * The max elapsed counter in the current time window
     */
    private final LongAccumulator maxElapsedAccumulator = new LongAccumulator(Long::max, 0);

    // === concurrent/maxConcurrent

    /**
     * The total concurrent exceed counter in the current time window
     */
    private final AtomicLong concurrentCounter = new AtomicLong(0);
    /**
     * The max concurrent counter in the current time window
     */
    private final LongAccumulator maxConcurrentAccumulator = new LongAccumulator(Long::max, 0);

    // === exception

    /**
     * The total exception counter in the current time window
     */
    private final LongAdder timeoutExceptionCounter = new LongAdder();
    /**
     * The total exception counter in the current time window
     */
    private final LongAdder rejectedExceptionCounter = new LongAdder();

    /**
     * The wrapper of original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    public Object wrapperOriginalCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: increment traffic
            incrementTraffic();
            // original call
            Object result = originalCall.call(limiterContext);
            // Step 2: success traffic
            successTraffic();
            return result;
        } catch (Throwable t) {
            // Step 3: total exception traffic
            exceptionTraffic(t);
            throw t;
        } finally {
            // Step 4 decrement traffic
            decrementTraffic(startTime);
        }
    }

    /**
     * The total increment of statistical traffic
     */
    private void incrementTraffic() {
        try {
            // increment request times
            requestCounter.increment();

            // increment concurrent times
            long concurrentNum = concurrentCounter.incrementAndGet();
            // total max concurrent times
            maxConcurrentAccumulator.accumulate(concurrentNum);
        } catch (Exception e) {
            log.error("The increment traffic is exception", e);
        }
    }

    /**
     * The success of statistical traffic
     */
    private void successTraffic() {
        try {
            // total all success times
            successCounter.increment();
        } catch (Exception e) {
            log.error("The total success traffic is exception", e);
        }
    }

    /**
     * The total exception of statistical traffic
     *
     * @param t {@link Throwable}
     */
    private void exceptionTraffic(Throwable t) {
        try {
            // total all failure times
            failureCounter.increment();
            if (t instanceof TimeoutException) {
                timeoutExceptionCounter.increment();
            } else if (t instanceof RejectedExecutionException) {
                rejectedExceptionCounter.increment();
            }
        } catch (Exception e) {
            log.error("The exception traffic is exception", e);
        }
    }

    /**
     * The total decrement of statistical traffic
     *
     * @param startTime start time
     */
    private void decrementTraffic(long startTime) {
        try {
            long elapsed = System.currentTimeMillis() - startTime;
            // total all elapsed
            totalElapsedAccumulator.accumulate(elapsed);
            // total max elapsed
            maxElapsedAccumulator.accumulate(elapsed);

            // decrement concurrent times
            concurrentCounter.decrementAndGet();
        } catch (Exception e) {
            log.error("The decrement traffic is exception", e);
        }
    }

    /**
     * The get statistics and reset
     *
     * @return statistics data map
     */
    public Map<String, Long> getAndReset() {
        final Map<String, Long> map = new LinkedHashMap<>();
        // reset number
        long success = successCounter.sumThenReset();
        long request = requestCounter.sumThenReset();
        long failure = failureCounter.sumThenReset();
        // reset elapsed
        long avgElapsed = success <= 0 ? 0 : (totalElapsedAccumulator.getThenReset() / success);
        long maxElapsed = maxElapsedAccumulator.getThenReset();
        // reset concurrent
        long concurrent = concurrentCounter.get();
        long maxConcurrent = maxConcurrentAccumulator.getThenReset();
        // reset exception
        long timeout = timeoutExceptionCounter.sumThenReset();
        long rejected = rejectedExceptionCounter.sumThenReset();
        // reset exceed
        long concurrentExceed = concurrentExceedCounter.sumThenReset();
        long rateExceed = rateExceedCounter.sumThenReset();
        if (request < 1 || success < 1) {
            return map;
        }

        // statistics number
        map.put(SUCCESS_KEY, success);
        map.put(REQUEST_KEY, request);
        map.put(FAILURE_KEY, failure);
        // statistics elapsed
        map.put(AVG_ELAPSED_KEY, avgElapsed);
        map.put(MAX_ELAPSED_KEY, maxElapsed);
        // statistics concurrent
        map.put(CONCURRENT_KEY, concurrent);
        map.put(MAX_CONCURRENT_KEY, maxConcurrent);
        // statistics exception
        map.put(TIMEOUT_TIMES_KEY, timeout);
        map.put(REJECTED_TIMES_KEY, rejected);
        // statistics exceed
        map.put(CONCURRENT_EXCEED_KEY, concurrentExceed);
        map.put(RATE_EXCEED_KEY, rateExceed);
        return map;
    }

    /**
     * The get statistics data
     *
     * @return statistics data map
     */
    public Map<String, Long> getStatisticsData() {
        final Map<String, Long> map = new LinkedHashMap<>();
        // statistics trade
        long success = successCounter.sum();
        map.put(SUCCESS_KEY, success);
        map.put(REQUEST_KEY, requestCounter.sum());
        map.put(FAILURE_KEY, failureCounter.sum());
        // statistics elapsed
        map.put(AVG_ELAPSED_KEY, success <= 0 ? 0 : (totalElapsedAccumulator.get() / success));
        map.put(MAX_ELAPSED_KEY, maxElapsedAccumulator.get());
        // statistics concurrent
        map.put(CONCURRENT_KEY, concurrentCounter.get());
        map.put(MAX_CONCURRENT_KEY, maxConcurrentAccumulator.get());
        // statistics exception
        map.put(TIMEOUT_TIMES_KEY, timeoutExceptionCounter.sum());
        map.put(REJECTED_TIMES_KEY, rejectedExceptionCounter.sum());
        // statistics exceed
        map.put(CONCURRENT_EXCEED_KEY, concurrentExceedCounter.sum());
        map.put(RATE_EXCEED_KEY, rateExceedCounter.sum());
        return map;
    }

}
