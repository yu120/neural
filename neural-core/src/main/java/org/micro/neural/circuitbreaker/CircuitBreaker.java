package org.micro.neural.circuitbreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Circuit Breaker
 *
 * @author lry
 */
public class CircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

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
    private AtomicInteger consecutiveSuccCount = new AtomicInteger(0);

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

    //状态操作

    /**
     * closed -> open | half open -> open
     */
    public void open() {
        lastOpenedTime = System.currentTimeMillis();
        state = CircuitBreakerState.OPEN;
        logger.debug("circuit open,key:{}", name);
    }

    /**
     * open -> half open
     */
    public void openHalf() {
        consecutiveSuccCount.set(0);
        state = CircuitBreakerState.HALF_OPEN;
        logger.debug("circuit open-half,key:{}", name);
    }

    /**
     * half open -> close
     */
    public void close() {
        failCount.reset();
        state = CircuitBreakerState.CLOSED;
        logger.debug("circuit close,key:{}", name);
    }

    //阈值判断

    /**
     * 是否应该转到half open
     * 前提是 open state
     *
     * @return
     */
    public boolean isOpen2HalfOpenTimeout() {
        return System.currentTimeMillis() - config.getOpen2HalfOpenTimeoutInMs() > lastOpenedTime;
    }

    /**
     * 是否应该从close转到open
     *
     * @return
     */
    public boolean isCloseFailThresholdReached() {
        return failCount.thresholdReached();
    }

    /**
     * half-open状态下是否达到close的阈值
     *
     * @return
     */
    public boolean isConsecutiveSuccessThresholdReached() {
        return consecutiveSuccCount.get() >= config.getConsecutiveSuccThreshold();
    }

    public void incrFailCount() {
        int count = failCount.incrAndGet();
        logger.debug("incr fail count:{},key:{}", count, name);
    }

    public AtomicInteger getConsecutiveSuccCount() {
        return consecutiveSuccCount;
    }

    public CircuitBreakerState getState() {
        return state;
    }

}
