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
    public synchronized Map<String, Long> getAndReset() {
        Map<String, Long> map = super.getAndReset();
        if (map == null || map.isEmpty()) {
            return map;
        }

        // statistics exceed
        map.put(DEGRADE_TIMES_KEY, counter.sumThenReset());

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

        // statistics trade
        map.put(DEGRADE_TIMES_KEY, counter.sum());

        return map;
    }

}
