package cn.micro.neural.limiter;

import lombok.*;

import java.io.Serializable;

/**
 * The Global Config of Limiter.
 *
 * @author lry
 */
@Data
public class LimiterGlobalConfig implements Serializable {

    private static final long serialVersionUID = -9072659813214931506L;

    public static final String IDENTITY = "limiter";

    /**
     * The switch, default is Switch.ON
     **/
    protected Switch enable = Switch.ON;
    /**
     * The broadcast event, default is Switch.ON
     */
    protected Switch broadcastEvent = Switch.ON;

    /**
     * The report cycle of monitor statistics(ms)
     */
    protected Long statisticReportCycle = 10 * 1000L;
    /**
     * The statistic data expire(ms)
     */
    protected Long statisticExpire = 3 * 60 * 1000L;

    /**
     * The Switch.
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Switch {

        /**
         * The switch is OFF
         */
        OFF("The switch is OFF"),
        /**
         * The switch is ON
         */
        ON("The switch is ON");

        String message;

    }

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