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
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LimiterStatistics extends GlobalStatistics {

    private static final long serialVersionUID = 5685475274387172658L;

    /**
     * The total concurrency exceed counter in the current time window
     */
    private final LongAdder concurrencyExceedCounter = new LongAdder();
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
                    concurrencyExceedCounter.increment();
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
    public synchronized Map<String, Long> getAndReset(String identity, Long statisticReportCycle) {
        Long time = super.buildStatisticsTime(statisticReportCycle);
        Map<String, Long> map = super.getAndReset(identity, time);

        // statistics exceed
        long concurrencyExceed = concurrencyExceedCounter.sumThenReset();
        long rateExceed = rateExceedCounter.sumThenReset();
        if (map.isEmpty()) {
            return map;
        }

        // statistics exceed
        map.put(String.format(STATISTICS, CONCURRENCY_EXCEED_KEY, identity, time), concurrencyExceed);
        map.put(String.format(STATISTICS, RATE_EXCEED_KEY, identity, time), rateExceed);

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
        // statistics trade
        map.put(CONCURRENCY_EXCEED_KEY, concurrencyExceedCounter.longValue());
        map.put(RATE_EXCEED_KEY, rateExceedCounter.longValue());

        return map;
    }

}
