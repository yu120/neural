package cn.neural.common.utils;

import com.sun.management.OperatingSystemMXBean;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * SystemUtils
 * <p>
 * 1.EnvironmentInfo
 * 2.MemoryInfo
 * 3.CpuLoadInfo
 * 4.DiskInfo
 * 5.GarbageCollectorInfo
 *
 * @author lry
 */
@Slf4j
public class SystemUtils {

    /**
     * The collect
     *
     * @return {@link MetricCollect}
     */
    public static MetricCollect collect() {
        MetricCollect metricCollect = new MetricCollect();
        metricCollect.setMemoryInfo(SystemUtils.getMemory());
        metricCollect.setCpuLoadInfo(SystemUtils.getCpuLoadInfo());
        metricCollect.setDiskInfo(SystemUtils.getDiskInfo());
        metricCollect.setGarbageCollectorInfos(SystemUtils.getGarbageCollectorInfo());
        return metricCollect;
    }

    @Data
    public static class MetricCollect implements Serializable {
        private static final long serialVersionUID = 3465584618686630943L;
        private SystemUtils.MemoryInfo memoryInfo;
        private SystemUtils.CpuLoadInfo cpuLoadInfo;
        private List<DiskInfo> diskInfo;
        private List<GarbageCollectorInfo> garbageCollectorInfos;
    }

    /**
     * The get environment info
     *
     * @return {@link EnvironmentInfo}
     */
    public static EnvironmentInfo getEnvironment() {
        EnvironmentInfo info = new EnvironmentInfo();
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));
        info.setOsArch(System.getProperty("os.arch"));
        info.setJvmName(System.getProperty("java.vm.name"));
        info.setJvmVersion(System.getProperty("java.runtime.version"));
        info.setCpuCore(Runtime.getRuntime().availableProcessors());

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        if (network != null) {
                            info.setNetworkName(network.getName());
                            info.setNetworkDisplayName(network.getDisplayName());
                        }
                    } catch (Throwable e) {
                        log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("Failed to retrieving ip address, " + e.getMessage(), e);
        }

        return info;
    }


    /**
     * The get memory info
     *
     * @return {@link MemoryInfo}
     */
    public static MemoryInfo getMemory() {
        int mb = 1024 * 1024;
        Runtime runtime = Runtime.getRuntime();
        MemoryInfo memoryInfo = new MemoryInfo();

        // JVM
        double freeMemory = (double) runtime.freeMemory() / mb;
        double maxMemory = (double) runtime.maxMemory() / mb;
        double totalMemory = (double) runtime.totalMemory() / mb;
        double usedMemory = totalMemory - freeMemory;
        double percentUsed = (usedMemory / maxMemory) * 100.0;
        usedMemory = new BigDecimal(usedMemory).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        maxMemory = new BigDecimal(maxMemory).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        percentUsed = new BigDecimal(percentUsed).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        memoryInfo.setUsedMemory(usedMemory);
        memoryInfo.setMaxMemory(maxMemory);
        memoryInfo.setPercentUsed(percentUsed);

        // System
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double totalPhysicalMemory = (double) osBean.getTotalPhysicalMemorySize() / mb;
        double freePhysicalMemory = (double) osBean.getFreePhysicalMemorySize() / mb;
        double committedVirtualMemory = (double) osBean.getCommittedVirtualMemorySize() / mb;
        totalPhysicalMemory = new BigDecimal(totalPhysicalMemory).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        freePhysicalMemory = new BigDecimal(freePhysicalMemory).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        committedVirtualMemory = new BigDecimal(committedVirtualMemory).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        memoryInfo.setTotalPhysicalMemory(totalPhysicalMemory);
        memoryInfo.setFreePhysicalMemory(freePhysicalMemory);
        memoryInfo.setCommittedVirtualMemory(committedVirtualMemory);

        return memoryInfo;
    }

    /**
     * The get cpu load info
     *
     * @return {@link CpuLoadInfo}
     */
    public static CpuLoadInfo getCpuLoadInfo() {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = ObjectName.getInstance("java.lang:type=OperatingSystem");

            // read process cpu load
            Double processCpuLoad = getAttributeValue(beanServer, objectName, "ProcessCpuLoad");
            // read system cpu load
            Double systemCpuLoad = getAttributeValue(beanServer, objectName, "SystemCpuLoad");
            if (processCpuLoad != null || systemCpuLoad != null) {
                CpuLoadInfo cpuLoadInfo = new CpuLoadInfo();
                cpuLoadInfo.setProcessCpuLoad(processCpuLoad);
                cpuLoadInfo.setSystemCpuLoad(systemCpuLoad);
                return cpuLoadInfo;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * The get disk info
     *
     * @return {@link DiskInfo}
     */
    public static List<DiskInfo> getDiskInfo() {
        double mb = 1024 * 1024;

        List<DiskInfo> diskInfoList = new ArrayList<>();
        File[] disks = File.listRoots();
        for (File file : disks) {
            DiskInfo diskInfo = new DiskInfo();
            diskInfo.setPath(file.getPath());
            // 空闲空间
            diskInfo.setFreeSpace(new BigDecimal(
                    file.getFreeSpace() / mb).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            // 可用空间
            diskInfo.setUsableSpace(new BigDecimal(
                    file.getUsableSpace() / mb).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            // 总空间
            diskInfo.setTotalSpace(new BigDecimal(
                    file.getTotalSpace() / mb).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            diskInfoList.add(diskInfo);
        }

        return diskInfoList;
    }

    /**
     * The get garbage collector info
     *
     * @return {@link GarbageCollectorInfo}
     */
    public static List<GarbageCollectorInfo> getGarbageCollectorInfo() {
        List<GarbageCollectorInfo> garbageCollectorInfoList = new ArrayList<>();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            GarbageCollectorInfo garbageCollectorInfo = new GarbageCollectorInfo();
            garbageCollectorInfo.setName(gcBean.getName());
            garbageCollectorInfo.setCollectionTime(gcBean.getCollectionTime());
            garbageCollectorInfo.setCollectionCount(gcBean.getCollectionCount());
            garbageCollectorInfoList.add(garbageCollectorInfo);
        }

        return garbageCollectorInfoList;
    }

    private static Double getAttributeValue(MBeanServer beanServer, ObjectName objectName, String attribute) throws Exception {
        Object cpuLoadObject = beanServer.getAttribute(objectName, attribute);
        if (cpuLoadObject == null) {
            return null;
        }

        double cpuLoadValue = (double) cpuLoadObject;
        if (cpuLoadValue < 0) {
            // usually takes a couple of seconds before we get real values
            return 0.00;
        }

        // returns a percentage value with 1 decimal point precision
        return new BigDecimal(cpuLoadValue * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * EnvironmentInfo
     *
     * @author lry
     */
    @Data
    @ToString
    public static class EnvironmentInfo implements Serializable {

        private static final long serialVersionUID = -9055644935967288508L;

        private String osName;
        private String osVersion;
        private String osArch;
        private String jvmName;
        private String jvmVersion;
        private Integer cpuCore;
        private String networkName;
        private String networkDisplayName;
    }

    /**
     * MemoryInfo
     *
     * @author lry
     */
    @Data
    @ToString
    public static class MemoryInfo implements Serializable {

        private static final long serialVersionUID = -6078242692181489693L;

        // === JVM
        /**
         * Used memory(MB)
         */
        private double usedMemory;
        /**
         * Max memory(MB)
         */
        private double maxMemory;
        /**
         * Percent used(%)
         */
        private double percentUsed;

        // === System

        private double totalPhysicalMemory;
        private double freePhysicalMemory;
        private double committedVirtualMemory;

    }

    /**
     * CpuLoadInfo
     *
     * @author lry
     */
    @Data
    @ToString
    public static class CpuLoadInfo implements Serializable {

        private static final long serialVersionUID = -7983527685877649306L;

        /**
         * Process CPU load
         */
        private Double processCpuLoad;
        /**
         * System CPU load
         */
        private Double systemCpuLoad;

    }

    /**
     * DiskInfo
     *
     * @author lry
     */
    @Data
    @ToString
    public static class DiskInfo implements Serializable {

        private static final long serialVersionUID = 1486150261930978720L;

        private String path;
        private Double freeSpace;
        private Double usableSpace;
        private Double totalSpace;
    }

    /**
     * GarbageCollectorInfo
     *
     * @author lry
     */
    @Data
    @ToString
    public static class GarbageCollectorInfo implements Serializable {

        private static final long serialVersionUID = -6536126923166050353L;

        private String name;
        private Long collectionTime;
        private Long collectionCount;
    }

}
