package cn.micro.neural.circuitbreaker;

import cn.micro.neural.circuitbreaker.exception.CircuitBreakerOpenException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreakerFactory {

    private CircuitBreaker circuitBreaker;
    private Class<? extends Throwable>[] ignoreExceptions;

    /**
     * The process of original call
     *
     * @param limiterContext {@link LimiterContext}
     * @param identity
     * @param originalCall   {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    public Object originalCall(final LimiterContext limiterContext, String identity, OriginalCall originalCall) throws Throwable {
        LimiterContext.set(limiterContext);

        try {
            if (circuitBreaker.isClosed()) {
                return processClose(identity, limiterContext, originalCall);
            } else if (circuitBreaker.isOpen()) {
                return processOpen(identity, limiterContext, originalCall);
            } else if (circuitBreaker.isHalfOpen()) {
                return processHalfOpen(identity, limiterContext, originalCall);
            } else {
                throw new IllegalArgumentException("");
            }
        } finally {
            LimiterContext.remove();
        }
    }

    /**
     * Close state processing
     *
     * @param identity
     * @param limiterContext
     * @param originalCall
     * @return
     * @throws Throwable
     */
    private Object processClose(String identity, LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        try {
            Object result = originalCall.call(limiterContext);
            // Reset the state and data to prevent accidents
            circuitBreaker.close();
            return result;
        } catch (Throwable t) {
            if (isIgnoreException(t)) {
                // Skip ignored exceptions, do not count
                throw t;
            }

            // Increase count
            circuitBreaker.incrFailCount();

            // Check if you should go from ‘close’ to ‘open’
            if (circuitBreaker.isCloseFailThresholdReached()) {
                // Trigger threshold, open fuse
                log.debug("[{}] reached fail threshold, circuit breaker open.", identity);
                circuitBreaker.open();
                throw new CircuitBreakerOpenException(identity);
            }

            throw t;
        }
    }

    /**
     * Open state processing
     *
     * @param identity
     * @param limiterContext
     * @param originalCall
     * @return
     * @throws Throwable
     */
    private Object processOpen(String identity, LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        // Check if you should enter the half open state
        if (circuitBreaker.isOpen2HalfOpenTimeout()) {
            log.debug("[{}] into half open", identity);

            // Enter half open state
            circuitBreaker.openHalf();

            // process half open
            return processHalfOpen(identity, limiterContext, originalCall);
        }

        throw new CircuitBreakerOpenException(identity);
    }

    /**
     * Half-open state processing
     *
     * @param identity
     * @param limiterContext
     * @param originalCall
     * @return
     * @throws Throwable
     */
    private Object processHalfOpen(String identity, LimiterContext limiterContext, OriginalCall originalCall) throws Throwable {
        try {
            // try to release the request
            Object result = originalCall.call(limiterContext);

            // Record the number of consecutive successes in the half-open state, and failures are immediately cleared
            circuitBreaker.getConsecutiveSuccessCount().incrementAndGet();

            // Whether the close threshold is reached in half-open state
            if (circuitBreaker.isConsecutiveSuccessThresholdReached()) {
                // If the call is successful, it will enter the close state
                circuitBreaker.close();
            }

            return result;
        } catch (Throwable t) {
            if (isIgnoreException(t)) {
                circuitBreaker.getConsecutiveSuccessCount().incrementAndGet();
                if (circuitBreaker.isConsecutiveSuccessThresholdReached()) {
                    circuitBreaker.close();
                }

                throw t;
            } else {
                circuitBreaker.open();
                throw new CircuitBreakerOpenException(identity, t);
            }
        }
    }

    private boolean isIgnoreException(Throwable t) {
        if (ignoreExceptions == null || ignoreExceptions.length == 0) {
            return false;
        }

        for (Class<? extends Throwable> ex : ignoreExceptions) {
            //是否是抛出异常t的父类
            //t java.lang.reflect.InvocationTargetException
            if (ex.isAssignableFrom(t.getCause().getClass())) {
                return true;
            }
        }

        return false;
    }

}
