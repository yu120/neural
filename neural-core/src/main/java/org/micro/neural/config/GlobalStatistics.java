package org.micro.neural.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.NeuralContext;
import org.micro.neural.OriginalCall;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import static org.micro.neural.common.Constants.*;

/**
 * The Global Statistics.
 * <p>
 * TODO: 使用Redis存储短期内精准数据,便于实现自动化的治理打下基础？
 *
 * @author lry
 **/
@Slf4j
@Data
public class GlobalStatistics implements Serializable {

    private static final long serialVersionUID = 2972356738274634556L;

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
    /**
     * The total timeout counter in the current time window
     */
    private final LongAdder timeoutCounter = new LongAdder();
    /**
     * The total rejection counter in the current time window
     */
    private final LongAdder rejectionCounter = new LongAdder();
    /**
     * The total sql exception counter in the current time window
     */
    private final LongAdder sqlExceptionCounter = new LongAdder();
    /**
     * The total runtime exception counter in the current time window
     */
    private final LongAdder runtimeExceptionCounter = new LongAdder();

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

    /**
     * The wrapper of original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    public Object wrapperOriginalCall(NeuralContext neuralContext, OriginalCall originalCall) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: increment traffic
            incrementTraffic();
            // original call
            Object result = originalCall.call();
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
                // total all timeout times
                timeoutCounter.increment();
            } else if (t instanceof RejectedExecutionException) {
                // total all rejection times
                rejectionCounter.increment();
            } else if (t instanceof SQLException) {
                // total all sql exception times
                sqlExceptionCounter.increment();
            } else if (t instanceof RuntimeException) {
                // total all runtime exception times
                runtimeExceptionCounter.increment();
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
    protected Map<String, Long> getAndReset() {
        Map<String, Long> map = new LinkedHashMap<>();
        // reset number
        long success = successCounter.sumThenReset();
        long request = requestCounter.sumThenReset();
        long failure = failureCounter.sumThenReset();
        long timeout = timeoutCounter.sumThenReset();
        long rejection = rejectionCounter.sumThenReset();
        // reset elapsed
        long avgElapsed = success <= 0 ? 0 : (totalElapsedAccumulator.getThenReset() / success);
        long maxElapsed = maxElapsedAccumulator.getThenReset();
        // reset concurrent
        long concurrent = concurrentCounter.get();
        long maxConcurrent = maxConcurrentAccumulator.getThenReset();
        if (request < 1 || success < 1) {
            return map;
        }

        // statistics number
        map.put(SUCCESS_KEY, success);
        map.put(REQUEST_KEY, request);
        map.put(FAILURE_KEY, failure);
        map.put(TIMEOUT_KEY, timeout);
        map.put(REJECTION_KEY, rejection);
        // statistics elapsed
        map.put(AVG_ELAPSED_KEY, avgElapsed);
        map.put(MAX_ELAPSED_KEY, maxElapsed);
        // statistics concurrent
        map.put(CONCURRENT_KEY, concurrent);
        map.put(MAX_CONCURRENT_KEY, maxConcurrent);

        return map;
    }

    /**
     * The get statistics data
     *
     * @return statistics data map
     */
    public Map<String, Long> getStatisticsData() {
        Map<String, Long> map = new LinkedHashMap<>();

        // statistics trade
        long success = successCounter.sum();
        map.put(SUCCESS_KEY, success);
        map.put(REQUEST_KEY, requestCounter.sum());
        map.put(FAILURE_KEY, failureCounter.sum());
        map.put(TIMEOUT_KEY, timeoutCounter.sum());
        map.put(REJECTION_KEY, rejectionCounter.sum());
        // statistics elapsed
        map.put(AVG_ELAPSED_KEY, success <= 0 ? 0 : (totalElapsedAccumulator.get() / success));
        map.put(MAX_ELAPSED_KEY, maxElapsedAccumulator.get());
        // statistics concurrent
        map.put(CONCURRENT_KEY, concurrentCounter.get());
        map.put(MAX_CONCURRENT_KEY, maxConcurrentAccumulator.get());

        return map;
    }

}
