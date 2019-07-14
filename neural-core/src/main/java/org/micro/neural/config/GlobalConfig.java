package org.micro.neural.config;

import lombok.*;

import java.io.Serializable;

/**
 * The Global Config.
 *
 * @author lry
 **/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class GlobalConfig implements Serializable {

    private static final long serialVersionUID = 3749338575377195865L;

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
     * The category of config
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Category {

        /**
         * The rule config
         */
        RULE("The rule config"),
        /**
         * The global config
         */
        GLOBAL("he global config");

        String message;

    }

    /**
     * The Model.
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Model {

        /**
         * The stand-alone model
         */
        STAND_ALONE("stand-alone"),
        /**
         * The cluster model
         */
        CLUSTER("cluster");

        String model;

    }

}