package cn.micro.neural.limiter;

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

    /**
     * The rate exceed event
     */
    RATE_EXCEED(EventType.IDENTITY, "The rate exceed event"),
    /**
     * The concurrent exceed event
     */
    CONCURRENT_EXCEED(EventType.IDENTITY, "The concurrent exceed event"),
    /**
     * The request exceed event
     */
    REQUEST_EXCEED(EventType.IDENTITY, "The request exceed event"),
    /**
     * The notify config exception
     */
    NOTIFY_EXCEPTION(EventType.IDENTITY, "The notify config is exception"),
    /**
     * The collect statistics exception
     */
    COLLECT_EXCEPTION(EventType.IDENTITY, "The collect statistics is exception");

    public static final String IDENTITY = "limiter";

    private final String module;
    private final String message;

}