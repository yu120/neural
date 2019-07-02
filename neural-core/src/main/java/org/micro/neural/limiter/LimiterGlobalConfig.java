package org.micro.neural.limiter;

import lombok.*;
import org.micro.neural.config.GlobalConfig;
import org.micro.neural.config.event.IEventType;

/**
 * The Global Config of Limiter.
 *
 * @author lry
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LimiterGlobalConfig extends GlobalConfig {

    private static final long serialVersionUID = -9072659813214931506L;

    public static final String IDENTITY = "limiter";

    /**
     * The Limiter Type.
     *
     * @author lry
     **/
    @Getter
    @AllArgsConstructor
    public enum EventType implements IEventType {

        /**
         * The rate exceed event
         */
        RATE_EXCEED(IDENTITY, "The rate exceed event"),
        /**
         * The concurrent exceed event
         */
        CONCURRENT_EXCEED(IDENTITY, "The concurrent exceed event"),
        /**
         * The request exceed event
         */
        REQUEST_EXCEED(IDENTITY, "The request exceed event"),
        /**
         * The notify config exception
         */
        NOTIFY_EXCEPTION(IDENTITY, "The notify config is exception"),
        /**
         * The collect statistics exception
         */
        COLLECT_EXCEPTION(IDENTITY, "The collect statistics is exception");

        String module;
        String message;

    }

}