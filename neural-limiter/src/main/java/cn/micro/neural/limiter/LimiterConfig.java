package cn.micro.neural.limiter;

import cn.micro.neural.limiter.exception.LimiterException;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * LimiterConfig
 *
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
public class LimiterConfig implements Serializable {

    private static final long serialVersionUID = -2617753757420740743L;

    public static String DELIMITER = ":";
    public static final String DEFAULT_NODE = "limiter";
    public static final String DEFAULT_APPLICATION = "micro";
    public static final String DEFAULT_GROUP = "neural";


    // === limiter config identity

    /**
     * The node name or id
     **/
    private String node = DEFAULT_NODE;
    /**
     * The application name or id
     **/
    private String application = DEFAULT_APPLICATION;
    /**
     * The group of service resource
     **/
    private String group = DEFAULT_GROUP;
    /**
     * The service key or resource key
     **/
    private String tag;

    // === limiter config intro

    /**
     * The switch of, default is Switch.ON
     **/
    private Switch enable = Switch.ON;
    /**
     * The limit name
     **/
    private String name;
    /**
     * The limit label list of limiter
     */
    private List<String> labels = new ArrayList<>();
    /**
     * The limit intro
     **/
    private String intro;

    // === limiter config strategy

    /**
     * The model of limiter
     */
    private Mode mode = Mode.STAND_ALONE;
    /**
     * The rate limiter
     */
    private RateLimiterConfig rate = new RateLimiterConfig();
    /**
     * The counter limiter
     */
    private CounterLimiterConfig counter = new CounterLimiterConfig();
    /**
     * The concurrent limiter
     */
    private ConcurrentLimiterConfig concurrent = new ConcurrentLimiterConfig();

    /**
     * Get config identity key
     *
     * @return config identity key
     */
    public String identity() {
        if (Stream.of(node, application, group, tag).anyMatch(s -> s.contains(DELIMITER))) {
            throw new LimiterException("The identity key can't include ':'");
        }

        return String.join(DELIMITER, node, application, group, tag);
    }

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

        private final String message;
    }

    /**
     * The Mode
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Mode {
        /**
         * The stand-alone model
         */
        STAND_ALONE("stand-alone", "Stand-alone mode"),
        /**
         * The cluster model
         */
        CLUSTER("cluster", "Cluster mode");

        private final String value;
        private final String message;
    }

    /**
     * The Strategy of Limiter Overflow.
     *
     * @author lry
     */
    @Getter
    @AllArgsConstructor
    public enum Strategy {
        /**
         * The ignore of limiter, when over flow
         */
        IGNORE("The ignore of limiter, when over flow"),
        /**
         * The fallback of limiter, when over flow
         */
        FALLBACK("The fallback of limiter, when over flow"),
        /**
         * The throw 'LimiterExceedException' exception of limiter, when over flow
         */
        EXCEPTION("The throw 'LimiterExceedException' exception of limiter, when over flow");

        private final String message;
    }

    /**
     * ConcurrentConfig
     *
     * @author lry
     */
    @Data
    public static class ConcurrentLimiterConfig implements Serializable {

        private static final long serialVersionUID = -5671416423715135681L;

        /**
         * Concurrent limiter switch, default is Switch.ON
         **/
        private Switch enable = Switch.ON;
        /**
         * The concurrent permit unit of concurrent limiter
         */
        private Integer permitUnit = 1;
        /**
         * The max concurrent number of concurrent limiter
         */
        private Integer maxPermit = 200;
        /**
         * The concurrent timeout of concurrent limiter.
         * <p>
         * The timeout time is 1ms, indicating the instantaneous concurrent statistics.
         */
        private Long timeout = 1L;
        /**
         * The strategy of limiter, default is Strategy.NON
         */
        private Strategy strategy = Strategy.IGNORE;

    }

    /**
     * RateConfig
     *
     * @author lry
     */
    @Data
    public static class RateLimiterConfig implements Serializable {

        private static final long serialVersionUID = -7307976708697925384L;

        /**
         * Rate limiter switch, default is Switch.ON
         **/
        private Switch enable = Switch.ON;
        /**
         * The rate of rate limiter
         */
        private Integer rateUnit = 1;
        /**
         * The max permit rate of rate limiter
         */
        private Integer maxRate = 1000;
        /**
         * The rate timeout of rate limiter
         * <p>
         * Count the number of requests within 1s by default.
         */
        private Long timeout = 1000L;
        /**
         * The strategy of limiter, default is Strategy.NON
         */
        private Strategy strategy = Strategy.IGNORE;

    }

    /**
     * CounterLimiterConfig
     *
     * @author lry
     */
    @Data
    public static class CounterLimiterConfig implements Serializable {

        private static final long serialVersionUID = -8642894858491116612L;

        /**
         * Counter limiter switch, default is Switch.ON
         **/
        private Switch enable = Switch.ON;
        /**
         * The count unit of counter limiter
         */
        private Integer countUnit = 1;
        /**
         * The max count of counter limiter
         */
        private Long maxCount = 1000L;
        /**
         * The counter timeout of counter limiter
         * <p>
         * The default count of requests within 60s.
         */
        private Long timeout = 60 * 1000L;
        /**
         * The strategy of limiter, default is Strategy.NON
         */
        private Strategy strategy = Strategy.IGNORE;

    }

}
