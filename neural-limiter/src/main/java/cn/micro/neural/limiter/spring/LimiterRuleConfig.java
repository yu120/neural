package cn.micro.neural.limiter.spring;

import cn.micro.neural.limiter.LimiterConfig.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * LimiterConfig
 *
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
@ConfigurationProperties("neural.limiter")
public class LimiterRuleConfig implements Serializable {

    private static final long serialVersionUID = -2617753757420740743L;

    public static final String DELIMITER = ":";


    // === limiter config identity

    /**
     * The node name or id
     **/
    private String node = "limiter";
    /**
     * The application name or id
     **/
    private String application = "micro";
    /**
     * The group of service resource
     **/
    private String group = "neural";
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
     * The strategy of limiter, default is Strategy.NON
     */
    private Strategy strategy = Strategy.NON;
    /**
     * The concurrent limiter
     */
    private ConcurrentLimiterConfig concurrent = new ConcurrentLimiterConfig();
    /**
     * The rate limiter
     */
    private RateLimiterConfig rate = new RateLimiterConfig();
    /**
     * The request limiter
     */
    private RequestLimiterConfig request = new RequestLimiterConfig();

}
