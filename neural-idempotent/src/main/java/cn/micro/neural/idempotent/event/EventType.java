package cn.micro.neural.idempotent.event;

import cn.micro.neural.idempotent.IdempotentFactory;
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
    CIRCUIT_BREAKER_CLOSED(IdempotentFactory.IDENTITY, "The circuit-breaker closed"),
    /**
     * The circuit-breaker half-open
     */
    CIRCUIT_BREAKER_HALF_OPEN(IdempotentFactory.IDENTITY, "The circuit-breaker half-open"),
    /**
     * The circuit-breaker open
     */
    CIRCUIT_BREAKER_OPEN(IdempotentFactory.IDENTITY, "The circuit-breaker open"),

    // === other

    /**
     * The refresh config exception
     */
    REFRESH_EXCEPTION(IdempotentFactory.IDENTITY, "The refresh config exception"),
    /**
     * The collect metric exception
     */
    COLLECT_EXCEPTION(IdempotentFactory.IDENTITY, "The collect metric exception"),
    /**
     * The statistics metric exception
     */
    STATISTICS_EXCEPTION(IdempotentFactory.IDENTITY, "The statistics metric exception");

    private final String category;
    private final String message;

}