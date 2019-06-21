package org.micro.neural.metric;

import org.micro.neural.extension.Extension;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 进程CPU使用率<br>
 * <br>
 * 一个监控CPU使用热点的MBean,一些指标报告可能不会对非热点JVM可用。<br>
 * CpuUsage – –目前使用的程序的CPU的百分比（平均测量之间）。<br>
 *
 * @author lry
 */
@Extension("cpuUsage")
public class CpuUsageMetric implements IMetric {

    private static final ObjectName RUNTIME_MBEAN;
    private static final String UPTIME_ATTR = "Uptime";
    private static final ObjectName OS_MBEAN;
    private static final String PROCESS_CPU_TIME_ATTR = "ProcessCpuTime";
    private static final String PROCESS_CPU_LOAD_ATTR = "ProcessCpuLoad";
    private static final String SYSTEM_CPU_LOAD_ATTR = "SystemCpuLoad";
    private static final String SYSTEM_LOAD_AVERAGE_ATTR = "SystemLoadAverage";

    //private static final 
    static {
        try {
            RUNTIME_MBEAN = new ObjectName("java.lang:type=Runtime");
            OS_MBEAN = new ObjectName("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private final MBeanServer mbeanServer;

    public CpuUsageMetric() {
        this(ManagementFactory.getPlatformMBeanServer());
    }

    public CpuUsageMetric(MBeanServer beanServer) {
        this.mbeanServer = beanServer;
    }

    @Override
    public Map<String, Object> getMetric() {
        final Map<String, Object> gauges = new HashMap<>();
        if (hasAttribute(OS_MBEAN, PROCESS_CPU_TIME_ATTR) && hasAttribute(RUNTIME_MBEAN, UPTIME_ATTR)) {
            long prevUptime = -1;
            long prevProcessCpuTime = -1;
            // final int processorCount = getAttributeInt(OS_MBEAN, AVAILABLE_PROCESSORS_ATTR);
            long uptime = getAttributeLong(RUNTIME_MBEAN, UPTIME_ATTR) * 1000000;
            long processCpuTime = getAttributeLong(OS_MBEAN, PROCESS_CPU_TIME_ATTR);

            double cpuUsage = 0.0;
            if (prevUptime != -1) {
                long uptimeDiff = uptime - prevUptime;
                long processTimeDiff = processCpuTime - prevProcessCpuTime;
                cpuUsage = (uptimeDiff > 0 ? (double) processTimeDiff / (double) uptimeDiff : 0) * 100.0;
            }

            prevUptime = uptime;
            prevProcessCpuTime = processCpuTime;
            gauges.put("cpu.process.cpuUsage", cpuUsage);
        }

        if (hasAttribute(OS_MBEAN, PROCESS_CPU_LOAD_ATTR)) {
            gauges.put("cpu.process.cpuLoad", getAttributeDouble(OS_MBEAN, PROCESS_CPU_LOAD_ATTR) * 100.0);
        }

        if (hasAttribute(OS_MBEAN, SYSTEM_LOAD_AVERAGE_ATTR)) {
            gauges.put("cpu.system.loadAverage", getAttributeDouble(OS_MBEAN, SYSTEM_LOAD_AVERAGE_ATTR) * 100.0);
        }

        if (hasAttribute(OS_MBEAN, SYSTEM_CPU_LOAD_ATTR)) {
            gauges.put("cpu.system.cpuLoad", getAttributeDouble(OS_MBEAN, SYSTEM_CPU_LOAD_ATTR) * 100.0);
        }

        return gauges;
    }

    private boolean hasAttribute(ObjectName mbean, String attr) {
        try {
            MBeanInfo info = mbeanServer.getMBeanInfo(mbean);
            for (MBeanAttributeInfo ai : info.getAttributes()) {
                if (attr.equals(ai.getName())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private long getAttributeLong(ObjectName mbean, String attr) {
        try {
            return (Long) mbeanServer.getAttribute(mbean, attr);
        } catch (Exception e) {
            throw new RuntimeException("Could not get attribute " + attr + " from MBean " + mbean, e);
        }
    }

    private double getAttributeDouble(ObjectName mbean, String attr) {
        try {
            return (Double) mbeanServer.getAttribute(mbean, attr);
        } catch (Exception e) {
            throw new RuntimeException("Could not get attribute " + attr + " from MBean " + mbean, e);
        }
    }

}
