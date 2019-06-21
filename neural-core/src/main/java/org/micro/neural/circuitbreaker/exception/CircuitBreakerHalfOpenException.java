package org.micro.neural.circuitbreaker.exception;

/**
 * The Circuit Breaker Half Open Exception
 *
 * @author lry
 */
public class CircuitBreakerHalfOpenException extends CircuitBreakerException {

    public CircuitBreakerHalfOpenException(String message) {
        super(message);
    }

    public CircuitBreakerHalfOpenException(String message, Throwable cause) {
        super(message, cause);
    }

}
