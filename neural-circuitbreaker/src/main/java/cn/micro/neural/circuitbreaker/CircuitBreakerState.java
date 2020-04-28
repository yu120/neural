package cn.micro.neural.circuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CircuitBreakerState
 *
 * @author lry
 */
@Getter
@AllArgsConstructor
public enum CircuitBreakerState {

    /**
     * working normally, calls are transparently passing through
     */
    CLOSED(-1, "close"),
    /**
     * method calls are passing through;
     * if another blacklisted exception is thrown, reverts back to OPEN
     */
    HALF_OPEN(0, "half-open"),
    /**
     * method calls are being intercepted and CircuitBreakerExceptions are being thrown instead
     */
    OPEN(1, "open");

    private final int value;
    private final String msg;

}
