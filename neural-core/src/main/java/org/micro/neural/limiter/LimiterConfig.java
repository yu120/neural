package org.micro.neural.limiter;

import lombok.*;
import org.micro.neural.config.RuleConfig;

/**
 * The Limiter Config.
 *
 * @author lry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LimiterConfig extends RuleConfig {

    private static final long serialVersionUID = 4076904823256002967L;

    /**
     * The rate of limiter
     */
    private Long rate = 0L;
    /**
     * The timeout of rate limiter
     */
    private Long rateTimeout = 0L;
    /**
     * The granularity of limiter, default is 1
     */
    private Long granularity = 1L;
    /**
     * The unit of limiter granularity, default is Unit.SEC
     */
    private Unit unit = Unit.SEC;

    /**
     * The concurrency of limiter
     */
    private Long concurrency = 0L;
    /**
     * The timeout of concurrency limiter
     */
    private Long concurrencyTimeout = 0L;

    /**
     * The strategy of limiter, default is Strategy.NON
     */
    private Strategy strategy = Strategy.NON;

    /**
     * The Strategy of Limiter Overflow.
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Strategy {

        /**
         * The skip of limiter, when over flow
         */
        NON("The skip of limiter, when over flow"),
        /**
         * The fallback of limiter, when over flow
         */
        FALLBACK("The fallback of limiter, when over flow"),
        /**
         * The throw 'LimiterExcessException' exception of limiter, when over flow
         */
        EXCEPTION("The throw 'LimiterExcessException' exception of limiter, when over flow");

        String message;

    }

    /**
     * The unit of limiter granularity
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Unit {

        /**
         * The second of limiter granularity unit, abbreviation 'SEC'
         */
        SEC("The second of limiter granularity unit, abbreviation 'SEC'"),
        /**
         * The minute of limiter granularity unit, abbreviation 'MIN'
         */
        MIN("The minute of limiter granularity unit, abbreviation 'MIN'"),
        /**
         * The hour of limiter granularity unit, abbreviation 'HOU'
         */
        HOU("The hour of limiter granularity unit, abbreviation 'HOU'"),
        /**
         * The day of limiter granularity unit, abbreviation 'DAY'
         */
        DAY("The day of limiter granularity unit, abbreviation 'DAY'");

        String message;

    }

}
