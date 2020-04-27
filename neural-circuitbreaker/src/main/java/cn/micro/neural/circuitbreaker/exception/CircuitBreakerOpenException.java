package cn.micro.neural.circuitbreaker.exception;

/**
 * CircuitBreakerOpenException
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
