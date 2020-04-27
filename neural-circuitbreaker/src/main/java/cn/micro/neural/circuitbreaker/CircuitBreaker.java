package cn.micro.neural.circuitbreaker;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Circuit Breaker
 *
 * @author lry
 */
@Slf4j
public class CircuitBreaker {

    private String name;
    private CircuitBreakerConfig config;
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    /**
     * 最近进入open状态的时间
     */
    private volatile long lastOpenedTime;
    /**
     * closed状态下失败次数
     */
    private LimitCounter failCount;
    /**
     * half-open状态的连续成功次数,失败立即清零
     */
    private AtomicInteger consecutiveSuccessCount = new AtomicInteger(0);


    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.config = config;
        this.name = name;
        failCount = new LimitCounter(config.getFailCountWindowInMs(), config.getFailThreshold());
    }

    public boolean isOpen() {
        return CircuitBreakerState.OPEN == state;
    }

    public boolean isHalfOpen() {
        return CircuitBreakerState.HALF_OPEN == state;
    }

    public boolean isClosed() {
        return CircuitBreakerState.CLOSED == state;
    }

    // === 状态操作

    /**
     * closed->open或half open->open
     */
    public void open() {
        lastOpenedTime = System.currentTimeMillis();
        state = CircuitBreakerState.OPEN;
        log.debug("circuit open,key:{}", name);
    }

    /**
     * open->half open
     */
    public void openHalf() {
        consecutiveSuccessCount.set(0);
        state = CircuitBreakerState.HALF_OPEN;
        log.debug("circuit open-half,key:{}", name);
    }

    /**
     * half open->close
     */
    public void close() {
        failCount.reset();
        state = CircuitBreakerState.CLOSED;
        log.debug("circuit close,key:{}", name);
    }

    // === 阈值判断

    /**
     * 是否应该转到half open
     * 前提是 open state
     */
    public boolean isOpen2HalfOpenTimeout() {
        return System.currentTimeMillis() - config.getOpen2HalfOpenTimeoutInMs() > lastOpenedTime;
    }

    /**
     * 是否应该从close转到open
     */
    public boolean isCloseFailThresholdReached() {
        return failCount.thresholdReached();
    }

    /**
     * half-open状态下是否达到close的阈值
     */
    public boolean isConsecutiveSuccessThresholdReached() {
        return consecutiveSuccessCount.get() >= config.getConsecutiveSuccThreshold();
    }

    public void incrFailCount() {
        int count = failCount.incrAndGet();
        log.debug("incr fail count:{},key:{}", count, name);
    }

    public AtomicInteger getConsecutiveSuccessCount() {
        return consecutiveSuccessCount;
    }

    public CircuitBreakerState getState() {
        return state;
    }

}
