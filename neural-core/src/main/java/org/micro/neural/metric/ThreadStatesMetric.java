package org.micro.neural.metric;

import org.micro.neural.extension.Extension;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 线程的指标收集器
 * <br>
 *
 * @author lry
 */
@Extension("thread")
public class ThreadStatesMetric implements IMetric {

    /**
     * do not compute stack traces.
     */
    private final static int STACK_TRACE_DEPTH = 0;

    private final ThreadMXBean threads;
    private final ThreadDeadlockDetector deadlockDetector;

    /**
     * Creates a new set of gauges using the default MXBeans.
     */
    public ThreadStatesMetric() {
        this(ManagementFactory.getThreadMXBean(), new ThreadDeadlockDetector());
    }

    /**
     * Creates a new set of gauges using the given MXBean and detector.
     *
     * @param threads          a thread MXBean
     * @param deadlockDetector a deadlock detector
     */
    public ThreadStatesMetric(ThreadMXBean threads, ThreadDeadlockDetector deadlockDetector) {
        this.threads = threads;
        this.deadlockDetector = deadlockDetector;
    }

    @Override
    public Map<String, Object> getMetric() {
        final Map<String, Object> gauges = new HashMap<>();
        for (final Thread.State state : Thread.State.values()) {
            gauges.put("thread_" + state.toString().toLowerCase() + "_count", getThreadCount(state));
        }

        gauges.put("thread_count", threads.getThreadCount());
        gauges.put("thread_daemon_count", threads.getDaemonThreadCount());
        gauges.put("thread_deadlock_count", deadlockDetector.getDeadlockedThreads().size());

        return Collections.unmodifiableMap(gauges);
    }

    private int getThreadCount(Thread.State state) {
        final ThreadInfo[] allThreads = getThreadInfo();
        int count = 0;
        for (ThreadInfo info : allThreads) {
            if (info != null && info.getThreadState() == state) {
                count++;
            }
        }
        return count;
    }

    private ThreadInfo[] getThreadInfo() {
        return threads.getThreadInfo(threads.getAllThreadIds(), STACK_TRACE_DEPTH);
    }

}
