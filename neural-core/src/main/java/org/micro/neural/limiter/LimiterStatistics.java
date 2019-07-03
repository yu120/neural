package org.micro.neural.limiter;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.config.GlobalStatistics;

import static org.micro.neural.common.Constants.*;

/**
 * The statistics of Limiter.
 *
 * @author lry
 */
@Data
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LimiterStatistics extends GlobalStatistics {

    private static final long serialVersionUID = -7032404135074863381L;

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
    public void exceedTraffic(LimiterGlobalConfig.EventType eventType) {
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

    /**
     * The get statistics and reset
     *
     * @return statistics data map
     */
    @Override
    public synchronized Map<String, Long> getAndReset() {
        Map<String, Long> map = super.getAndReset();

        // reset exceed
        long concurrentExceed = concurrentExceedCounter.sumThenReset();
        long rateExceed = rateExceedCounter.sumThenReset();
        if (map == null || map.isEmpty()) {
            return map;
        }

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
    @Override
    public Map<String, Long> getStatisticsData() {
        Map<String, Long> map = super.getStatisticsData();
        if (map == null || map.isEmpty()) {
            return map;
        }

        // statistics exceed
        map.put(CONCURRENT_EXCEED_KEY, concurrentExceedCounter.sum());
        map.put(RATE_EXCEED_KEY, rateExceedCounter.sum());
        return map;
    }

}
