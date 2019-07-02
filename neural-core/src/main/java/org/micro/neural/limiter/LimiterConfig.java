package org.micro.neural.limiter;

import lombok.*;
import org.micro.neural.config.RuleConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
     * The model of limiter
     */
    private String model = "stand-alone";

    // === concurrent limiter

    /**
     * The enable of concurrent limiter
     */
    private Boolean concurrentEnable = true;
    /**
     * The concurrent permit unit of concurrent limiter
     */
    private Integer concurrentPermit = 1;
    /**
     * The max concurrent number of concurrent limiter
     */
    private Integer maxPermitConcurrent = 200;
    /**
     * The concurrent timeout of concurrent limiter
     */
    private Long concurrentTimeout = 0L;

    // === rate limiter

    /**
     * The enable of rate limiter
     */
    private Boolean rateEnable = true;
    /**
     * The rate of rate limiter
     */
    private Integer ratePermit = 1;
    /**
     * The max permit rate of rate limiter
     */
    private Integer maxPermitRate = 1000;
    /**
     * The rate timeout of rate limiter
     */
    private Long rateTimeout = 0L;

    // === request limiter

    /**
     * The enable of request limiter
     */
    private Boolean requestEnable = true;
    /**
     * The request of rate limiter
     */
    private Integer requestPermit = 1;
    /**
     * The request max permit of request limiter
     */
    private Long maxPermitRequest = 1000L;
    /**
     * The request timeout of request limiter
     */
    private Long requestTimeout = 0L;
    /**
     * The request interval(windows) of request limiter
     */
    private Duration requestInterval = Duration.ofSeconds(60);

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
         * The throw 'LimiterExceedException' exception of limiter, when over flow
         */
        EXCEPTION("The throw 'LimiterExceedException' exception of limiter, when over flow");

        String message;
    }

}
