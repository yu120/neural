package cn.micro.neural.circuitbreaker.event;

import cn.micro.neural.circuitbreaker.CircuitBreakerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The circuit-breaker event type.
 *
 * @author lry
 **/
@Getter
@AllArgsConstructor
public enum EventType {

    // === circuit-breaker state

    /**
     * The circuit-breaker closed
     */
    CIRCUIT_BREAKER_CLOSED(CircuitBreakerFactory.IDENTITY, "The circuit-breaker closed"),
    /**
     * The circuit-breaker half-open
     */
    CIRCUIT_BREAKER_HALF_OPEN(CircuitBreakerFactory.IDENTITY, "The circuit-breaker half-open"),
    /**
     * The circuit-breaker open
     */
    CIRCUIT_BREAKER_OPEN(CircuitBreakerFactory.IDENTITY, "The circuit-breaker open"),

    // === other

    /**
     * The refresh config exception
     */
    REFRESH_EXCEPTION(CircuitBreakerFactory.IDENTITY, "The refresh config exception"),
    /**
     * The collect metric exception
     */
    COLLECT_EXCEPTION(CircuitBreakerFactory.IDENTITY, "The collect metric exception"),
    /**
     * The statistics metric exception
     */
    STATISTICS_EXCEPTION(CircuitBreakerFactory.IDENTITY, "The statistics metric exception");

    private final String category;
    private final String message;

}