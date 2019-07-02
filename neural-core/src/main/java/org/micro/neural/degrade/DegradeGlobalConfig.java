package org.micro.neural.degrade;

import lombok.*;
import org.micro.neural.config.GlobalConfig;
import org.micro.neural.config.event.IEventType;

/**
 * The Degrade Global Config
 *
 * @author lry
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DegradeGlobalConfig extends GlobalConfig {

    private static final long serialVersionUID = 6752689785921541587L;

    public static final String IDENTITY = "degrade";
    
    /**
     * The degrade level
     */
    private Level level = Level.NON;

    /**
     * The degrade level
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Level {

        /**
         * The no open downgrade, is non
         */
        NON(0, "The no open downgrade, is non"),
        /**
         * The may need to downgrade, is hint
         */
        HINT(1, "The may need to downgrade, is hint"),
        /**
         * The recommended to downgrade
         */
        RECOMMEND(2, "The recommended to downgrade"),
        /**
         * The need to downgrade
         */
        NEED(3, "The need to downgrade"),
        /**
         * The must to downgrade, is warning
         */
        WARN(4, "The must to downgrade, is warning"),
        /**
         * The must to downgrade, is serious
         */
        SERIOUS(5, "The must to downgrade, is serious");

        int order;
        String message;

    }

    /**
     * The Degrade Event Type.
     *
     * @author lry
     **/
    @Getter
    @AllArgsConstructor
    public enum EventType implements IEventType {

        /**
         * The notification mock data exception
         */
        NOTIFY_EXCEPTION(IDENTITY, "The notify config is exception"),
        /**
         * The collect statistics exception of degrade
         */
        COLLECT_EXCEPTION(IDENTITY, "The collect statistics is exception");

        String module;
        String message;

    }

}
