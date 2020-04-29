package cn.micro.neural.circuitbreaker.event;

import cn.micro.neural.circuitbreaker.CircuitBreakerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Limiter Type.
 *
 * @author lry
 **/
@Getter
@AllArgsConstructor
public enum EventType {

    // === rate limiter

    /**
     * The rate exceed
     */
    RATE_EXCEED(CircuitBreakerFactory.IDENTITY, "The rate exceed"),
    /**
     * The rate exception
     */
    RATE_EXCEPTION(CircuitBreakerFactory.IDENTITY, "The rate exception"),

    // === concurrent limiter

    /**
     * The concurrent exceed
     */
    CONCURRENT_EXCEED(CircuitBreakerFactory.IDENTITY, "The concurrent exceed"),
    /**
     * The concurrent exception
     */
    CONCURRENT_EXCEPTION(CircuitBreakerFactory.IDENTITY, "The concurrent exception"),

    // === counter limiter

    /**
     * The counter exceed
     */
    COUNTER_EXCEED(CircuitBreakerFactory.IDENTITY, "The counter exceed"),
    /**
     * The counter exception
     */
    COUNTER_EXCEPTION(CircuitBreakerFactory.IDENTITY, "The counter exception"),

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