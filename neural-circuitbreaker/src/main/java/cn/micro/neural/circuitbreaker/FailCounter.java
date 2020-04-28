package cn.micro.neural.circuitbreaker;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于固定时间窗口失败次数计数器,用于熔断器阈值判断
 *
 * @author lry
 */
public class FailCounter {

    /**
     * 开始时间
     */
    private long startTime;
    /**
     * closed状态的失败次数阈值
     */
    private int failThreshold;
    /**
     * closed状态的失败计数的时间窗口
     */
    private long failCountWindowInMs;
    /**
     * 当前失败计数器
     */
    private AtomicLong currentFailCounter;

    public FailCounter(int failThreshold, long failCountWindowInMs) {
        this.startTime = System.currentTimeMillis();
        this.failThreshold = failThreshold;
        this.failCountWindowInMs = failCountWindowInMs;
        this.currentFailCounter = new AtomicLong(0);
    }

    /**
     * 增加计数器并返回最新值
     *
     * @return 最新计数值
     */
    public long incrementAndGet() {
        long currentTime = System.currentTimeMillis();

        // 校验是否该重置时间窗的开始时间和计数器: 时间窗超时则自动重置开始时间和统计次数
        if ((startTime + failCountWindowInMs) < currentTime) {
            synchronized (this) {
                if ((startTime + failCountWindowInMs) < currentTime) {
                    startTime = currentTime;
                    currentFailCounter.set(0);
                }
            }
        }

        return currentFailCounter.incrementAndGet();
    }

    /**
     * 判断是否超过允许的最大失败次数
     *
     * @return true表示超过最大失败次数
     */
    public boolean thresholdReached() {
        return currentFailCounter.get() > failThreshold;
    }

    /**
     * 获取当前失败次数
     *
     * @return 失败次数
     */
    public long get() {
        return currentFailCounter.get();
    }

    /**
     * 重置
     */
    public void reset() {
        currentFailCounter.set(0);
    }

}
