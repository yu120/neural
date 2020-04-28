package cn.micro.neural.circuitbreaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

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
    private AtomicInteger consecutiveSuccessCounter = new AtomicInteger(0);


    public StandAloneCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) {
        super(circuitBreakerConfig);
        this.failCounter = new FailCounter(circuitBreakerConfig.getFailThreshold(), circuitBreakerConfig.getFailCountWindowInMs());
    }

    @Override
    protected void incrFailCounter() {
        failCounter.incrementAndGet();
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
        log.debug("Circuit-Breaker[{}] open", circuitBreakerConfig.getIdentity());
    }

    @Override
    public void openHalf() {
        consecutiveSuccessCounter.set(0);
        state = CircuitBreakerState.HALF_OPEN;
        log.debug("Circuit-Breaker[{}] open-half", circuitBreakerConfig.getIdentity());
    }

    @Override
    public void close() {
        failCounter.reset();
        state = CircuitBreakerState.CLOSED;
        log.debug("Circuit-Breaker[{}] close", circuitBreakerConfig.getIdentity());
    }

    // === 判断熔断状态是否该转移(即判断是否达到了转移的阈值)

    @Override
    public boolean isOpen2HalfOpenTimeout() {
        return System.currentTimeMillis() - lastOpenedTime > circuitBreakerConfig.getOpen2HalfOpenTimeoutInMs();
    }

    @Override
    public boolean isCloseFailThresholdReached() {
        return failCounter.thresholdReached();
    }

    @Override
    public boolean isConsecutiveSuccessThresholdReached() {
        return consecutiveSuccessCounter.get() >= circuitBreakerConfig.getConsecutiveSuccessThreshold();
    }

}
