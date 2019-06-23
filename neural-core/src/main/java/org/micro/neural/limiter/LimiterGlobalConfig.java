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

    private static final long serialVersionUID = -4103412814609503453L;
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
         * The rate exceed event of limiter
         */
        RATE_EXCEED(IDENTITY, "The rate exceed event of limiter"),
        /**
         * The concurrent exceed event of limiter
         */
        CONCURRENT_EXCEED(IDENTITY, "The concurrent exceed event of limiter"),
        /**
         * The notify config exception of limiter
         */
        NOTIFY_EXCEPTION(IDENTITY, "The notify config is exception of limiter"),
        /**
         * The collect statistics exception of limiter
         */
        COLLECT_EXCEPTION(IDENTITY, "The collect statistics is exception of limiter");

        String module;
        String message;

    }

}