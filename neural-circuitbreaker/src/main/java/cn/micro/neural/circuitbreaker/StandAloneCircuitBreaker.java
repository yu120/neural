package cn.micro.neural.circuitbreaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Circuit Breaker
 *
 * @author lry
 */
@Slf4j
@Getter
public class StandAloneCircuitBreaker extends AbstractCircuitBreaker {

    /**
     * 最近进入open状态的时间
     */
    private volatile long lastOpenedTime;
    /**
     * Circuit-Breaker state
     */
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    /**
     * half-open状态的连续成功次数,失败立即清零
     */
    private AtomicInteger consecutiveSuccessCounter = new AtomicInteger(0);

    // === 基于固定时间窗口失败次数计数器,用于熔断器阈值判断。closed状态下失败次数

    /**
     * 开始时间
     */
    private long failStartTime;
    /**
     * 当前失败计数器
     */
    private AtomicLong currentFailCounter;


    public StandAloneCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) {
        super(circuitBreakerConfig);
        this.failStartTime = System.currentTimeMillis();
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
        if ((failStartTime + circuitBreakerConfig.getFailCountWindowInMs()) < currentTime) {
            synchronized (this) {
                if ((failStartTime + circuitBreakerConfig.getFailCountWindowInMs()) < currentTime) {
                    failStartTime = currentTime;
                    currentFailCounter.set(0);
                }
            }
        }

        return currentFailCounter.incrementAndGet();
    }

    @Override
    protected void incrFailCounter() {
        currentFailCounter.incrementAndGet();
    }

    @Override
    protected void incrConsecutiveSuccessCounter() {
        consecutiveSuccessCounter.incrementAndGet();
    }

    // === 状态操作

    @Override
    public void open() {
        lastOpenedTime = System.currentTimeMillis();
        state = CircuitBreakerState.OPEN;
        log.debug("Circuit-breaker[{}] open", circuitBreakerConfig.getIdentity());
    }

    @Override
    public void openHalf() {
        consecutiveSuccessCounter.set(0);
        state = CircuitBreakerState.HALF_OPEN;
        log.debug("Circuit-beaker[{}] open-half", circuitBreakerConfig.getIdentity());
    }

    @Override
    public void close() {
        // 重置失败次数统计器
        currentFailCounter.set(0);
        state = CircuitBreakerState.CLOSED;
        log.debug("Circuit-breaker[{}] close", circuitBreakerConfig.getIdentity());
    }

    // === 判断熔断状态是否该转移(即判断是否达到了转移的阈值)

    @Override
    public boolean isOpen2HalfOpenTimeout() {
        return System.currentTimeMillis() - lastOpenedTime > circuitBreakerConfig.getOpen2HalfOpenTimeoutInMs();
    }

    @Override
    public boolean isCloseFailThresholdReached() {
        // 判断是否超过允许的最大失败次数,true表示超过最大失败次数
        return currentFailCounter.get() > circuitBreakerConfig.getFailThreshold();
    }

    @Override
    public boolean isConsecutiveSuccessThresholdReached() {
        return consecutiveSuccessCounter.get() >= circuitBreakerConfig.getConsecutiveSuccessThreshold();
    }

}
