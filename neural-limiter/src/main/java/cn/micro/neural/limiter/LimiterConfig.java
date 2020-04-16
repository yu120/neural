package cn.micro.neural.limiter;

import lombok.*;

import java.io.Serializable;
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
public class LimiterConfig implements Serializable {

    private static final long serialVersionUID = 4076904823256002967L;
    public static final String DELIMITER = ":";

    /**
     * The module name or id
     **/
    private String module;

    /**
     * The application name or id
     **/
    private String application = "micro";
    /**
     * The group of service resource
     **/
    private String group = "neural";
    /**
     * The service or resource id
     **/
    private String resource;

    /**
     * The switch of, default is Switch.ON
     **/
    private Switch enable = Switch.ON;
    /**
     * The resource name
     **/
    private String name;
    /**
     * The tag list of limiter
     */
    private List<String> tags = new ArrayList<>();
    /**
     * The remarks
     **/
    private String remarks;

    public String identity() {
        if (module.contains(DELIMITER) ||
                application.contains(DELIMITER) ||
                group.contains(DELIMITER) ||
                resource.contains(DELIMITER)) {
            throw new IllegalArgumentException("The identity key can't include ':'");
        }

        return (module + DELIMITER + application + DELIMITER + group + DELIMITER + resource).toUpperCase();
    }

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
