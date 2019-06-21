package org.micro.neural.circuitbreaker.exception;

/**
 * The Circuit Breaker Open Exception
 *
 * @author lry
 */
public class CircuitBreakerOpenException extends CircuitBreakerException {

    public CircuitBreakerOpenException(String message, Throwable cause) {
        super("The operation " + message + " has too many failures, tripping circuit breaker.", cause);
    }

    public CircuitBreakerOpenException(String message) {
        super("The operation " + message + " has too many failures, tripping circuit breaker.");
    }

}
