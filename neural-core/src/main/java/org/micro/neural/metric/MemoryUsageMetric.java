package org.micro.neural.metric;

import org.micro.neural.extension.Extension;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 堆与非堆内存指标收集器
 *
 * @author lry
 */
@Extension("memory")
public class MemoryUsageMetric implements IMetric {

    private final MemoryMXBean mxBean;

    public MemoryUsageMetric() {
        this.mxBean = ManagementFactory.getMemoryMXBean();
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
     * 将long型数值转换为字符串，单位从字节转换为MB
     */
    private static Double toMBStr(Long value) {
        return Double.valueOf(String.format("%.2f", (double) value / 1024 / 1024));
    }

    private static Double toPercentStr(long used, long max) {
        Double percent = (used * 100.0) / max;
        return Double.valueOf(String.format("%.2f", percent));
    }

    private Map<String, Double> getData() {
        final Map<String, Double> gauges = new LinkedHashMap<>();

        // 单位：MB,初始内存大小（包括堆和非堆）
        gauges.put("jvm_memory_init", toMBStr(mxBean.getHeapMemoryUsage().getInit() +
                mxBean.getNonHeapMemoryUsage().getInit()));
        // 单位：MB,已使用内存大小（包括堆和非堆）
        gauges.put("jvm_memory_used", toMBStr(mxBean.getHeapMemoryUsage().getUsed() +
                mxBean.getNonHeapMemoryUsage().getUsed()));
        // 单位：MB,内存最大值（包括堆和非堆）
        gauges.put("jvm_memory_max", toMBStr(mxBean.getHeapMemoryUsage().getMax() +
                mxBean.getNonHeapMemoryUsage().getMax()));
        // 单位：MB,内存提交值（包括堆和非堆）
        gauges.put("jvm_memory_committed", toMBStr(mxBean.getHeapMemoryUsage().getCommitted() +
                mxBean.getNonHeapMemoryUsage().getCommitted()));
        // 单位：MB,堆初始化大小
        gauges.put("jvm_heap_init", toMBStr(mxBean.getHeapMemoryUsage().getInit()));
        // 单位：MB,堆已使用量
        gauges.put("jvm_heap_used", toMBStr(mxBean.getHeapMemoryUsage().getUsed()));
        // 单位：MB,堆最大大小
        gauges.put("jvm_heap_max", toMBStr(mxBean.getHeapMemoryUsage().getMax()));
        // 单位：MB,堆内存提交值
        gauges.put("jvm_heap_committed", toMBStr(mxBean.getHeapMemoryUsage().getCommitted()));
        // 堆已使用百分比
        final MemoryUsage usage = mxBean.getHeapMemoryUsage();
        gauges.put("jvm_heap_usage", toPercentStr(usage.getUsed(), usage.getMax()));

        // 单位：MB,非堆初始化大小
        gauges.put("jvm_nonheap_init", toMBStr(mxBean.getNonHeapMemoryUsage().getInit()));
        // 单位：MB,非堆已使用量
        gauges.put("jvm_nonheap_used", toMBStr(mxBean.getNonHeapMemoryUsage().getUsed()));
        // 单位：MB,非堆最大大小
        gauges.put("jvm_nonheap_max", toMBStr(mxBean.getNonHeapMemoryUsage().getMax()));
        // 单位：MB,非堆内存提交值
        gauges.put("jvm_nonheap_committed", toMBStr(mxBean.getNonHeapMemoryUsage().getCommitted()));
        // 非堆已使用百分比
        final MemoryUsage noHeapUsage = mxBean.getNonHeapMemoryUsage();
        gauges.put("jvm_nonheap_usage", toPercentStr(noHeapUsage.getUsed(), noHeapUsage.getMax()));

        return gauges;
    }
}
