package org.micro.neural.degrade;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import lombok.*;
import org.micro.neural.common.Constants;
import org.micro.neural.config.statistics.Statistics;
import lombok.extern.slf4j.Slf4j;

/**
 * The Degrade Statistics.
 *
 * @author lry
 **/
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DegradeStatistics extends Statistics implements Serializable {

    private static final long serialVersionUID = -2928427414009752116L;

    /**
     * The total times degrade counter（ms）
     */
    private LongAdder counter = new LongAdder();

    /**
     * The get statistics data
     *
     * @return statistics data map
     */
    @Override
    public Map<String, Long> getStatisticsData() {
        Map<String, Long> map = super.getStatisticsData();
        // statistics trade
        map.put("counter", counter.longValue());

        return map;
    }

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
        map.put(String.format(Constants.TOTAL_DEGRADE, identity, time), totalDegrade);

        return map;
    }

}
