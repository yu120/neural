package org.micro.neural.metric;

import org.micro.neural.extension.Extension;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 虚拟机的类加载系统负载值
 * <br>
 * A set of gauges for JVM classloader usage.
 *
 * @author lry
 */
@Extension("classLoading")
public class ClassLoadMetric implements IMetric {

    private final ClassLoadingMXBean mxBean;

    public ClassLoadMetric() {
        this(ManagementFactory.getClassLoadingMXBean());
    }

    public ClassLoadMetric(ClassLoadingMXBean mxBean) {
        this.mxBean = mxBean;
    }

    @Override
    public Map<String, Object> getMetric() {
        final Map<String, Object> gauges = new HashMap<>();
        gauges.put("classload_totalLoadedCount", mxBean.getTotalLoadedClassCount());
        gauges.put("classload_unloadedCount", mxBean.getUnloadedClassCount());
        gauges.put("classload_loadedCount", mxBean.getLoadedClassCount());

        return Collections.unmodifiableMap(gauges);
    }
}
