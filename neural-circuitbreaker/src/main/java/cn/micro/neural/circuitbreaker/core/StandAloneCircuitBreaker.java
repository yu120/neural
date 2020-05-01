package cn.micro.neural.circuitbreaker.core;

import cn.micro.neural.circuitbreaker.CircuitBreakerConfig;
import cn.micro.neural.circuitbreaker.CircuitBreakerState;
import cn.neural.common.extension.Extension;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * StandAloneCircuitBreaker
 *
 * @author lry
 */
@Slf4j
@Getter
@Extension("stand-alone")
public class StandAloneCircuitBreaker extends AbstractCircuitBreaker {

    /**
     * 最近进入open状态的时间
     */
    private AtomicLong lastOpenedTime;
    /**
     * Circuit-Breaker state
     */
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    /**
     * half-open状态的连续成功次数,失败立即清零
     */
    private AtomicInteger consecutiveSuccessCounter = new AtomicInteger(0);

    // === 基于固定时间窗口的失败次数计数器

    /**
     * 开始时间
     */
    private AtomicLong failStartTime = new AtomicLong(System.currentTimeMillis());
    /**
     * 当前失败计数器
     */
    private AtomicLong failCounter = new AtomicLong(0);


    @Override
    protected boolean tryRefresh(CircuitBreakerConfig config) {
        return true;
    }

    @Override
    protected void incrFailCounter() {
        long currentTime = System.currentTimeMillis();

        // 校验是否该重置时间窗的开始时间和计数器: 时间窗超时则自动重置开始时间和统计次数
        if ((failStartTime.get() + config.getFailCountWindowInMs()) < currentTime) {
            synchronized (this) {
                if ((failStartTime.get() + config.getFailCountWindowInMs()) < currentTime) {
                    failStartTime.set(currentTime);
                    failCounter.set(0);
                }
            }
        }

        failCounter.incrementAndGet();
    }

    @Override
    protected void incrConsecutiveSuccessCounter() {
        consecutiveSuccessCounter.incrementAndGet();
    }

    // === 状态操作

    @Override
    public void open() {
        lastOpenedTime = new AtomicLong(System.currentTimeMillis());
        state = CircuitBreakerState.OPEN;
        log.debug("Circuit-breaker[{}] open", config.identity());
    }

    @Override
    public void openHalf() {
        consecutiveSuccessCounter.set(0);
        state = CircuitBreakerState.HALF_OPEN;
        log.debug("Circuit-beaker[{}] open-half", config.identity());
    }

    @Override
    public void close() {
        // 重置失败次数统计器
        failCounter.set(0);
        state = CircuitBreakerState.CLOSED;
        log.debug("Circuit-breaker[{}] close", config.identity());
    }

    // === 判断熔断状态是否该转移(即判断是否达到了转移的阈值)

    @Override
    public boolean isOpen2HalfOpenTimeout() {
        return System.currentTimeMillis() - config.getOpen2HalfOpenTimeoutInMs() > lastOpenedTime.get();
    }

    @Override
    public boolean isCloseFailThresholdReached() {
        // 判断是否超过允许的最大失败次数,true表示超过最大失败次数
        return failCounter.get() > config.getFailThreshold();
    }

    @Override
    public boolean isConsecutiveSuccessThresholdReached() {
        return consecutiveSuccessCounter.get() >= config.getConsecutiveSuccessThreshold();
    }

}
