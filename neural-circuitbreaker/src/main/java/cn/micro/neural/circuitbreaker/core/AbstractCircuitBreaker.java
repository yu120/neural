package cn.micro.neural.circuitbreaker.core;

import cn.micro.neural.circuitbreaker.CircuitBreakerConfig;
import cn.micro.neural.circuitbreaker.CircuitBreakerState;
import cn.micro.neural.circuitbreaker.CircuitBreakerStatistics;
import cn.micro.neural.circuitbreaker.event.EventListener;
import cn.micro.neural.circuitbreaker.event.EventType;
import cn.micro.neural.circuitbreaker.exception.CircuitBreakerOpenException;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import cn.neural.common.utils.BeanUtils;
import cn.neural.common.utils.CloneUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * AbstractCircuitBreaker
 *
 * @author lry
 */
@Slf4j
@Getter
public abstract class AbstractCircuitBreaker implements ICircuitBreaker {

    private final Set<EventListener> listeners = new LinkedHashSet<>();
    private final CircuitBreakerStatistics statistics = new CircuitBreakerStatistics();
    protected CircuitBreakerConfig config;

    public AbstractCircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
    }

    @Override
    public void addListener(EventListener... eventListeners) {
        Collections.addAll(listeners, eventListeners);
    }

    @Override
    public synchronized boolean refresh(CircuitBreakerConfig config) throws Exception {
        try {
            log.info("Refresh the current circuit-breaker config: {}", config);
            if (null == config || this.config.equals(config)) {
                return false;
            }

            // Copy properties attributes after deep copy
            BeanUtils.copyProperties(CloneUtils.clone(config), this.config);

            return tryRefresh(config);
        } catch (Exception e) {
            this.collectEvent(EventType.REFRESH_EXCEPTION);
            throw e;
        }
    }

    /**
     * The collect event
     *
     * @param eventType {@link EventType}
     * @param args      attachment parameters
     */
    private void collectEvent(EventType eventType, Object... args) {
        for (EventListener eventListener : listeners) {
            try {
                eventListener.onEvent(config, eventType, args);
            } catch (Exception e) {
                log.error("The collect event exception", e);
            }
        }
    }

    @Override
    public Object wrapperCall(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        OriginalContext.set(originalContext);

        try {
            if (CircuitBreakerState.CLOSED == getState()) {
                return processClose(originalContext, originalCall);
            } else if (CircuitBreakerState.OPEN == getState()) {
                return processOpen(originalContext, originalCall);
            } else if (CircuitBreakerState.HALF_OPEN == getState()) {
                return processHalfOpen(originalContext, originalCall);
            } else {
                throw new IllegalArgumentException("Illegal circuit-breaker state");
            }
        } finally {
            OriginalContext.remove();
        }
    }

    @Override
    public Map<String, Long> collect() {
        try {
            return statistics.collectThenReset();
        } catch (Exception e) {
            this.collectEvent(EventType.COLLECT_EXCEPTION);
            log.error("The limiter[{}] collect exception", config.identity(), e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> statistics() {
        try {
            return statistics.getStatisticsData();
        } catch (Exception e) {
            this.collectEvent(EventType.STATISTICS_EXCEPTION);
            log.error("The limiter[{}] statistics exception", config.identity(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Close state processing
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    private Object processClose(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        try {
            return originalCall.call(originalContext);
        } catch (Throwable t) {
            if (isIgnoreException(t)) {
                // Skip ignored exceptions, do not count
                throw t;
            }

            // 增量统计失败次数
            incrFailCounter();

            // Check if you should go from ‘close’ to ‘open’
            if (isCloseFailThresholdReached()) {
                // Trigger threshold, open fuse
                log.debug("[{}] reached fail threshold, circuit-breaker open.", config.identity());
                open();
                this.collectEvent(EventType.CIRCUIT_BREAKER_OPEN);
                throw new CircuitBreakerOpenException(config.identity());
            }

            throw t;
        }
    }

    /**
     * Open state processing
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    private Object processOpen(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        // Check if you should enter the half open state
        if (isOpen2HalfOpenTimeout()) {
            log.debug("[{}] into half open", config.identity());

            // Enter half open state
            openHalf();
            this.collectEvent(EventType.CIRCUIT_BREAKER_HALF_OPEN);

            // process half open
            return processHalfOpen(originalContext, originalCall);
        }

        throw new CircuitBreakerOpenException(config.identity());
    }

    /**
     * Half-open state processing
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    private Object processHalfOpen(OriginalContext originalContext, OriginalCall originalCall) throws Throwable {
        try {
            // try to release the request
            Object result = originalCall.call(originalContext);

            // Record the number of consecutive successes in the half-open state, and failures are immediately cleared
            incrConsecutiveSuccessCounter();

            // Whether the close threshold is reached in half-open state
            if (isConsecutiveSuccessThresholdReached()) {
                // If the call is successful, it will enter the close state
                close();
                this.collectEvent(EventType.CIRCUIT_BREAKER_CLOSED);
            }

            return result;
        } catch (Throwable t) {
            if (isIgnoreException(t)) {
                incrConsecutiveSuccessCounter();
                if (isConsecutiveSuccessThresholdReached()) {
                    close();
                }

                throw t;
            } else {
                open();
                this.collectEvent(EventType.CIRCUIT_BREAKER_OPEN);
                throw new CircuitBreakerOpenException(config.identity(), t);
            }
        }
    }

    /**
     * 是否是需要忽略的异常
     *
     * @param t {@link Throwable}
     * @return true表示需要忽略
     */
    private boolean isIgnoreException(Throwable t) {
        if (config.getIgnoreExceptions().size() == 0) {
            return false;
        }

        for (String exceptionClassName : config.getIgnoreExceptions()) {
            if (exceptionClassName.equals(t.getClass().getName())
                    || exceptionClassName.equals(t.getCause().getClass().getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * The do refresh
     *
     * @param config {@link CircuitBreakerConfig}
     * @return true is success
     */
    protected abstract boolean tryRefresh(CircuitBreakerConfig config);

    /**
     * 增量增加失败次数
     */
    protected abstract void incrFailCounter();

    /**
     * 增量增加连续成功次数
     */
    protected abstract void incrConsecutiveSuccessCounter();

}
