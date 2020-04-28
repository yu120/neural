package cn.micro.neural.circuitbreaker.core;

import cn.micro.neural.circuitbreaker.CircuitBreakerConfig;
import cn.micro.neural.circuitbreaker.CircuitBreakerState;
import cn.micro.neural.circuitbreaker.exception.CircuitBreakerOpenException;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * AbstractCircuitBreaker
 *
 * @author lry
 */
@Slf4j
@Getter
public abstract class AbstractCircuitBreaker implements ICircuitBreaker {

    protected CircuitBreakerConfig circuitBreakerConfig;

    public AbstractCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) {
        this.circuitBreakerConfig = circuitBreakerConfig;
    }

    /**
     * The process of original call
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    @Override
    public Object originalCall(final OriginalContext originalContext, final OriginalCall originalCall) throws Throwable {
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
            Object result = originalCall.call(originalContext);
            // Reset the state and data to prevent accidents
            close();
            return result;
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
                log.debug("[{}] reached fail threshold, circuit-breaker open.", circuitBreakerConfig.getIdentity());
                open();
                throw new CircuitBreakerOpenException(circuitBreakerConfig.getIdentity());
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
            log.debug("[{}] into half open", circuitBreakerConfig.getIdentity());

            // Enter half open state
            openHalf();

            // process half open
            return processHalfOpen(originalContext, originalCall);
        }

        throw new CircuitBreakerOpenException(circuitBreakerConfig.getIdentity());
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
                throw new CircuitBreakerOpenException(circuitBreakerConfig.getIdentity(), t);
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
        if (circuitBreakerConfig.getIgnoreExceptions().size() == 0) {
            return false;
        }

        for (String exceptionClassName : circuitBreakerConfig.getIgnoreExceptions()) {
            if (exceptionClassName.equals(t.getClass().getName())
                    || exceptionClassName.equals(t.getCause().getClass().getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 增量增加失败次数
     */
    protected abstract void incrFailCounter();

    /**
     * 增量增加连续成功次数
     */
    protected abstract void incrConsecutiveSuccessCounter();

}
