package org.micro.neural.metric;

import org.micro.neural.extension.Extension;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 垃圾回收的指标收集器
 *
 * @author lry
 */
@Extension("gc")
public class GarbageCollectorMetric implements IMetric {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private final List<GarbageCollectorMXBean> garbageCollectors;
    private static Map<String, Object> lastMetric = new LinkedHashMap<>();

    public GarbageCollectorMetric() {
        this.garbageCollectors = new ArrayList<>(ManagementFactory.getGarbageCollectorMXBeans());
    }

    @Override
    public Map<String, Object> getMetric() {
        final Map<String, Object> gauges = new HashMap<>();
        for (final String metricsKey : getData().keySet()) {
            gauges.put(metricsKey, getData().get(metricsKey));
        }

        return Collections.unmodifiableMap(gauges);
    }

    /**
     * ps_scavenge.count:新生代PS（并行扫描）次数
     * ps_scavenge.time:单位：秒,新生代PS（并行扫描）时间
     * ps_marksweep.count:老年代CMS（并行标记清扫）次数
     * ps_marksweep_time:单位：秒,老年代CMS（并行标记清扫）时间
     * <p>
     * ps_scavenge_diff_count:新生代PS（并行扫描）变化次数
     * ps_scavenge_diff_time: 单位：秒,新生代PS（并行扫描）变化时间
     * ps_marksweep_diff_count: 老年代CMS（并行标记清扫）变化次数
     * ps_marksweep_diff_time: 单位：秒,老年代CMS（并行标记清扫）变化时间
     */
    public Map<String, Double> getData() {
        final Map<String, Double> gauges = new LinkedHashMap<String, Double>();

        for (final GarbageCollectorMXBean gc : garbageCollectors) {
            final String name = "gc_" + WHITESPACE.matcher(gc.getName()).replaceAll("_").toLowerCase();

            String lastCountKey = name + "_diff_count";
            Object lastCountVal = lastMetric.get(lastCountKey);
            lastCountVal = (lastCountVal == null) ? 0 : lastCountVal;
            long lastCountCurrent = gc.getCollectionCount();
            long lastCountKv = lastCountCurrent - Long.valueOf(lastCountVal + "");
            lastMetric.put(lastCountKey, lastCountCurrent);
            gauges.put(lastCountKey, (double) lastCountKv);

            String lastTimeKey = name + "_diff_time";
            Object lastTimeVal = lastMetric.get(lastTimeKey);
            lastTimeVal = (lastTimeVal == null) ? 0 : lastTimeVal;
            Double lastTimeCurrent = (double) gc.getCollectionTime();
            double lastTimeKv = lastTimeCurrent - Double.valueOf(lastTimeVal + "");
            lastMetric.put(lastTimeKey, lastTimeCurrent);
            gauges.put(lastTimeKey, Double.valueOf(String.format("%.3f", lastTimeKv / 1000)));

            gauges.put(name + "_count", (double) lastCountCurrent);
            // 单位：从毫秒转换为秒
            gauges.put(name + "_time", Double.valueOf(String.format("%.3f", lastTimeCurrent / 1000)));
        }

        return gauges;
    }
}
