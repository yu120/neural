package cn.micro.neural.limiter.event;

import cn.micro.neural.limiter.LimiterFactory;
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
    RATE_EXCEED(LimiterFactory.IDENTITY, "The rate exceed"),
    /**
     * The rate exception
     */
    RATE_EXCEPTION(LimiterFactory.IDENTITY, "The rate exception"),

    // === concurrent limiter

    /**
     * The concurrent exceed
     */
    CONCURRENT_EXCEED(LimiterFactory.IDENTITY, "The concurrent exceed"),
    /**
     * The concurrent exception
     */
    CONCURRENT_EXCEPTION(LimiterFactory.IDENTITY, "The concurrent exception"),

    // === counter limiter

    /**
     * The counter exceed
     */
    COUNTER_EXCEED(LimiterFactory.IDENTITY, "The counter exceed"),
    /**
     * The counter exception
     */
    COUNTER_EXCEPTION(LimiterFactory.IDENTITY, "The counter exception"),

    // === other

    /**
     * The refresh config exception
     */
    REFRESH_EXCEPTION(LimiterFactory.IDENTITY, "The refresh config exception"),
    /**
     * The collect metric exception
     */
    COLLECT_EXCEPTION(LimiterFactory.IDENTITY, "The collect metric exception"),
    /**
     * The statistics metric exception
     */
    STATISTICS_EXCEPTION(LimiterFactory.IDENTITY, "The statistics metric exception");

    private final String category;
    private final String message;

}