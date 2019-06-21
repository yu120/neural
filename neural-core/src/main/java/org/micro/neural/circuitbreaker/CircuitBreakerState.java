package org.micro.neural.circuitbreaker;

/**
 * The Circuit Breaker State
 *
 * @author lry
 */
public enum CircuitBreakerState {

    /**
     * working normally, calls are transparently passing through
     */
    CLOSED,

    /**
     * method calls are being intercepted and CircuitBreakerExceptions are being thrown instead
     */
    OPEN,

    /**
     * method calls are passing through;
     * if another blacklisted exception is thrown, reverts back to OPEN
     */
    HALF_OPEN;

}
