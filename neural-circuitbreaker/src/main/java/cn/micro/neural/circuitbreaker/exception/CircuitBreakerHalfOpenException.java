package cn.micro.neural.circuitbreaker.exception;

/**
 * CircuitBreakerHalfOpenException
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
