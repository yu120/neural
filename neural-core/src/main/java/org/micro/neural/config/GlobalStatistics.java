package org.micro.neural.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.OriginalCall;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

import static org.micro.neural.common.Constants.*;

/**
 * The Global Statistics.
 *
 * @author lry
 **/
@Slf4j
@Data
public class GlobalStatistics implements Serializable {

    private static final long serialVersionUID = 2972356738274634556L;

    /**
     * The total request counter in the current time window: Calculation QPS
     */
    protected final LongAdder requestCounter = new LongAdder();

    /**
     * The total success counter in the current time window: Calculation TPS
     */
    protected final LongAdder successCounter = new LongAdder();
    /**
     * The total failure counter in the current time window
     */
    protected final LongAdder failureCounter = new LongAdder();

    /**
     * The total timeout counter in the current time window
     */
    protected final LongAdder timeoutCounter = new LongAdder();
    /**
     * The total rejection counter in the current time window
     */
    protected final LongAdder rejectionCounter = new LongAdder();

    /**
     * The total elapsed counter in the current time window
     */
    protected final LongAdder elapsedCounter = new LongAdder();
    /**
     * The max elapsed counter in the current time window
     */
    protected final LongAccumulator maxElapsedCounter = new LongAccumulator(Long::max, 0);

    /**
     * The total concurrency exceed counter in the current time window
     */
    protected final LongAdder concurrencyCounter = new LongAdder();
    /**
     * The max concurrency counter in the current time window
     */
    protected final LongAccumulator maxConcurrencyCounter = new LongAccumulator(Long::max, 0);

    /**
     * The total rate counter in the current time window
     */
    protected final LongAdder rateCounter = new LongAdder();
    /**
     * The max rate counter in the current time window
     */
    protected final LongAccumulator maxRateCounter = new LongAccumulator(Long::max, 0);

    /**
     * The total request of statistical traffic
     */
    public void totalRequestTraffic() {
        try {
            // increment request times
            requestCounter.increment();
        } catch (Exception e) {
            log.error("The total request traffic is exception", e);
        }
    }

    /**
     * The wrapper of original call
     *
     * @param originalCall The original call interface
     * @return The original call result
     * @throws Throwable throw original call exception
     */
    public Object wrapperOriginalCall(OriginalCall originalCall) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            // increment traffic
            incrementTraffic();
            // original call
            return originalCall.call();
        } catch (Throwable t) {
            // total exception traffic
            exceptionTraffic(t);
            throw t;
        } finally {
            // decrement traffic
            decrementTraffic(startTime);
        }
    }

    /**
     * The total increment of statistical traffic
     */
    private void incrementTraffic() {
        try {
            // increment concurrency times
            concurrencyCounter.increment();
            // total max concurrency times
            maxConcurrencyCounter.accumulate(concurrencyCounter.longValue());

            // increment request rate times
            rateCounter.increment();
            // total request max rate times
            maxRateCounter.accumulate(rateCounter.longValue());
        } catch (Exception e) {
            log.error("The increment traffic is exception", e);
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
            elapsedCounter.add(elapsed);
            // total max elapsed
            maxElapsedCounter.accumulate(elapsed);

            // total all success times
            successCounter.increment();
            // decrement concurrency times
            concurrencyCounter.decrement();
        } catch (Exception e) {
            log.error("The decrement traffic is exception", e);
        }
    }

    /**
     * The build statistics time
     *
     * @param statisticReportCycle statistic report cycle milliseconds
     * @return statistics time
     */
    protected long buildStatisticsTime(long statisticReportCycle) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        int second = (int) statisticReportCycle / 1000;
        int tempSecond = calendar.get(Calendar.SECOND) % second;
        second = tempSecond >= second / 2 ? second : 0;
        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + second - tempSecond);

        return calendar.getTimeInMillis();
    }

    /**
     * The get statistics and reset
     *
     * @return statistics data map
     */
    protected Map<String, Long> getAndReset(String identity, Long time) {
        Map<String, Long> map = new LinkedHashMap<>();
        // statistics trade
        long totalRequest = requestCounter.sumThenReset();
        long totalSuccess = successCounter.sumThenReset();
        long totalFailure = failureCounter.sumThenReset();
        // timeout/rejection
        long totalTimeout = timeoutCounter.sumThenReset();
        long totalRejection = rejectionCounter.sumThenReset();
        // statistics elapsed
        long totalElapsed = elapsedCounter.sumThenReset();
        long maxElapsed = maxElapsedCounter.getThenReset();
        // statistics concurrency
        long concurrency = concurrencyCounter.sumThenReset();
        long maxConcurrency = maxConcurrencyCounter.getThenReset();
        // statistics rate
        long rate = rateCounter.sumThenReset();
        long maxRate = maxRateCounter.getThenReset();

        if (totalRequest < 1) {
            return map;
        }

        // statistics trade
        map.put(String.format(STATISTICS, REQUEST_KEY, identity, time), totalRequest);
        map.put(String.format(STATISTICS, SUCCESS_KEY, identity, time), totalSuccess);
        map.put(String.format(STATISTICS, FAILURE_KEY, identity, time), totalFailure);
        // timeout/rejection
        map.put(String.format(STATISTICS, TIMEOUT_KEY, identity, time), totalTimeout);
        map.put(String.format(STATISTICS, REJECTION_KEY, identity, time), totalRejection);
        // statistics elapsed
        map.put(String.format(STATISTICS, ELAPSED_KEY, identity, time), totalElapsed);
        map.put(String.format(STATISTICS, MAX_ELAPSED_KEY, identity, time), maxElapsed);
        // statistics concurrency
        map.put(String.format(STATISTICS, CONCURRENCY_KEY, identity, time), concurrency);
        map.put(String.format(STATISTICS, MAX_CONCURRENCY_KEY, identity, time), maxConcurrency);
        // statistics concurrency
        map.put(String.format(STATISTICS, RATE_KEY, identity, time), rate);
        map.put(String.format(STATISTICS, MAX_RATE_KEY, identity, time), maxRate);

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
        map.put(REQUEST_KEY, requestCounter.longValue());
        map.put(SUCCESS_KEY, successCounter.longValue());
        map.put(FAILURE_KEY, failureCounter.longValue());
        // timeout/rejection
        map.put(TIMEOUT_KEY, timeoutCounter.longValue());
        map.put(REJECTION_KEY, rejectionCounter.longValue());
        // statistics elapsed
        map.put(ELAPSED_KEY, elapsedCounter.longValue());
        map.put(MAX_ELAPSED_KEY, maxElapsedCounter.longValue());
        // statistics concurrency
        map.put(CONCURRENCY_KEY, concurrencyCounter.longValue());
        map.put(MAX_CONCURRENCY_KEY, maxConcurrencyCounter.longValue());
        // statistics rate
        map.put(RATE_KEY, rateCounter.longValue());
        map.put(MAX_RATE_KEY, maxRateCounter.longValue());

        return map;
    }

}
