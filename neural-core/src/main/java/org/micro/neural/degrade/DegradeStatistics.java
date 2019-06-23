package org.micro.neural.degrade;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.micro.neural.config.GlobalStatistics;

import static org.micro.neural.common.Constants.*;

/**
 * The Degrade Statistics.
 *
 * @author lry
 **/
@Slf4j
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DegradeStatistics extends GlobalStatistics {

    private static final long serialVersionUID = -285179675871196824L;

    /**
     * The total times degrade counter（ms）
     */
    private final LongAdder counter = new LongAdder();

    /**
     * The query statistics and reset
     *
     * @return statistics data map
     */
    @Override
    public synchronized Map<String, Long> getAndReset(String identity, Long reportStatisticCycle) {
        Long time = super.buildStatisticsTime(reportStatisticCycle);
        Map<String, Long> map = super.getAndReset(identity, time);

        // statistics exceed
        long totalDegrade = counter.sumThenReset();
        if (map.isEmpty()) {
            return map;
        }

        // statistics exceed
        map.put(String.format(STATISTICS, DEGRADE_TIMES_KEY, identity, time), totalDegrade);

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
        map.put(DEGRADE_TIMES_KEY, counter.longValue());

        return map;
    }

}
