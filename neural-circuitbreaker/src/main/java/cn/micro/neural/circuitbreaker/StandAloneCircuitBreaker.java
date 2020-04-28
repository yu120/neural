package cn.micro.neural.circuitbreaker;

import cn.micro.neural.circuitbreaker.exception.CircuitBreakerOpenException;
import lombok.extern.slf4j.Slf4j;

/**
 * The Circuit Breaker
 *
 * @author lry
 */
@Slf4j
public class StandAloneCircuitBreaker extends AbstractCircuitBreaker {

    private Class<? extends Throwable>[] ignoreExceptions;

    public StandAloneCircuitBreaker(String name, CircuitBreakerConfig config) {
        super(name, config);
    }

    /**
     * The process of original call
     *
     * @param circuitBreakerContext {@link CircuitBreakerContext}
     * @param originalCall          {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    public Object originalCall(final CircuitBreakerContext circuitBreakerContext, final OriginalCall originalCall) throws Throwable {
        CircuitBreakerContext.set(circuitBreakerContext);

        try {
            if (super.isClosed()) {
                return processClose(circuitBreakerContext, originalCall);
            } else if (super.isOpen()) {
                return processOpen(circuitBreakerContext, originalCall);
            } else if (super.isHalfOpen()) {
                return processHalfOpen(circuitBreakerContext, originalCall);
            } else {
                throw new IllegalArgumentException("");
            }
        } finally {
            CircuitBreakerContext.remove();
        }
    }

    /**
     * Close state processing
     *
     * @param circuitBreakerContext {@link CircuitBreakerContext}
     * @param originalCall          {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    private Object processClose(CircuitBreakerContext circuitBreakerContext, OriginalCall originalCall) throws Throwable {
        try {
            Object result = originalCall.call(circuitBreakerContext);
            // Reset the state and data to prevent accidents
            super.close();
            return result;
        } catch (Throwable t) {
            if (isIgnoreException(t)) {
                // Skip ignored exceptions, do not count
                throw t;
            }

            // 增量统计失败次数
            super.getFailCounter().incrementAndGet();

            // Check if you should go from ‘close’ to ‘open’
            if (super.isCloseFailThresholdReached()) {
                // Trigger threshold, open fuse
                log.debug("[{}] reached fail threshold, circuit breaker open.", getIdentity());
                super.open();
                throw new CircuitBreakerOpenException(getIdentity());
            }

            throw t;
        }
    }

    /**
     * Open state processing
     *
     * @param circuitBreakerContext {@link CircuitBreakerContext}
     * @param originalCall          {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    private Object processOpen(CircuitBreakerContext circuitBreakerContext, OriginalCall originalCall) throws Throwable {
        // Check if you should enter the half open state
        if (super.isOpen2HalfOpenTimeout()) {
            log.debug("[{}] into half open", getIdentity());

            // Enter half open state
            super.openHalf();

            // process half open
            return processHalfOpen(circuitBreakerContext, originalCall);
        }

        throw new CircuitBreakerOpenException(getIdentity());
    }

    /**
     * Half-open state processing
     *
     * @param circuitBreakerContext {@link CircuitBreakerContext}
     * @param originalCall          {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    private Object processHalfOpen(CircuitBreakerContext circuitBreakerContext, OriginalCall originalCall) throws Throwable {
        try {
            // try to release the request
            Object result = originalCall.call(circuitBreakerContext);

            // Record the number of consecutive successes in the half-open state, and failures are immediately cleared
            super.getConsecutiveSuccessCount().incrementAndGet();

            // Whether the close threshold is reached in half-open state
            if (super.isConsecutiveSuccessThresholdReached()) {
                // If the call is successful, it will enter the close state
                super.close();
            }

            return result;
        } catch (Throwable t) {
            if (isIgnoreException(t)) {
                super.getConsecutiveSuccessCount().incrementAndGet();
                if (super.isConsecutiveSuccessThresholdReached()) {
                    super.close();
                }

                throw t;
            } else {
                super.open();
                throw new CircuitBreakerOpenException(getIdentity(), t);
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
        if (ignoreExceptions == null || ignoreExceptions.length == 0) {
            return false;
        }

        for (Class<? extends Throwable> ex : ignoreExceptions) {
            if (ex.isAssignableFrom(t.getClass()) || ex.isAssignableFrom(t.getCause().getClass())) {
                return true;
            }
        }

        return false;
    }

}
