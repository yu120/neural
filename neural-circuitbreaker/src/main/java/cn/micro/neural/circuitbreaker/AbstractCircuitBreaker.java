package cn.micro.neural.circuitbreaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AbstractCircuitBreaker
 *
 * @author lry
 */
@Slf4j
@Getter
public abstract class AbstractCircuitBreaker implements Serializable {

    /**
     * The identity key
     */
    private String identity;
    /**
     * Circuit-Breaker config
     */
    private CircuitBreakerConfig circuitBreakerConfig;
    /**
     * 最近进入open状态的时间
     */
    private volatile long lastOpenedTime;
    /**
     * closed状态下失败次数
     */
    private FailCounter failCounter;
    /**
     * Circuit-Breaker state
     */
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    /**
     * half-open状态的连续成功次数,失败立即清零
     */
    private AtomicInteger consecutiveSuccessCount = new AtomicInteger(0);


    public AbstractCircuitBreaker(String identity, CircuitBreakerConfig circuitBreakerConfig) {
        this.identity = identity;
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.failCounter = new FailCounter(circuitBreakerConfig.getFailThreshold(), circuitBreakerConfig.getFailCountWindowInMs());
    }

    // === 状态判断

    /**
     * 是否是打开状态
     *
     * @return true表示打开状态
     */
    public boolean isOpen() {
        return CircuitBreakerState.OPEN == state;
    }

    /**
     * 是否是半开状态
     *
     * @return true表示半开状态
     */
    public boolean isHalfOpen() {
        return CircuitBreakerState.HALF_OPEN == state;
    }

    /**
     * 是否是关闭状态
     *
     * @return true表示关闭状态
     */
    public boolean isClosed() {
        return CircuitBreakerState.CLOSED == state;
    }

    // === 状态操作

    /**
     * 打开熔断
     * <p>
     * 以下几种场景会使用打开操作：
     * 1.closed->open
     * 2.half-open->open
     */
    public void open() {
        lastOpenedTime = System.currentTimeMillis();
        state = CircuitBreakerState.OPEN;
        log.debug("Circuit-Breaker[{}] open", identity);
    }

    /**
     * 半开熔断
     * <p>
     * 以下几种场景会使用半开操作：
     * 1.open->half-open
     */
    public void openHalf() {
        consecutiveSuccessCount.set(0);
        state = CircuitBreakerState.HALF_OPEN;
        log.debug("Circuit-Breaker[{}] open-half", identity);
    }

    /**
     * 关闭熔断
     * <p>
     * 以下几种场景会使用关闭操作：
     * 1.half-open->close
     */
    public void close() {
        failCounter.reset();
        state = CircuitBreakerState.CLOSED;
        log.debug("Circuit-Breaker[{}] close", identity);
    }

    // === 判断熔断状态是否该转移(即判断是否达到了转移的阈值)

    /**
     * open状态下是否可以转移至half-open状态
     * <p>
     * 原理：当前时间和最后一次打开的时间差超过指定阈值(默认为5秒),则状态可以由打开转移为半开
     */
    public boolean isOpen2HalfOpenTimeout() {
        return System.currentTimeMillis() - lastOpenedTime > circuitBreakerConfig.getOpen2HalfOpenTimeoutInMs();
    }

    /**
     * close状态下是否可以转移至open状态
     * <p>
     * 原理：closed状态下判断是否超过允许的最大失败次数
     */
    public boolean isCloseFailThresholdReached() {
        return failCounter.thresholdReached();
    }

    /**
     * half-open状态下是否可以转移至close状态
     * <p>
     * 原理：half-open状态下连续成功次数超过配置的阈值,则可以转移至close状态
     */
    public boolean isConsecutiveSuccessThresholdReached() {
        return consecutiveSuccessCount.get() >= circuitBreakerConfig.getConsecutiveSuccessThreshold();
    }

}
