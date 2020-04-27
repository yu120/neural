package cn.micro.neural.circuitbreaker.exception;

/**
 * CircuitBreakerException
 *
 * @author lry
 */
public class CircuitBreakerException extends RuntimeException {

    public CircuitBreakerException(String message) {
        super(message);
    }

    public CircuitBreakerException(String message, Throwable cause) {
        super(message, cause);
    }

}
