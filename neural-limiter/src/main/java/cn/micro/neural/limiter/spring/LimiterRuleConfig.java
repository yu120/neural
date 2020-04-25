package cn.micro.neural.limiter.spring;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterConfig.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * LimiterRuleConfig
 *
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
@ConfigurationProperties("neural.limiter")
public class LimiterRuleConfig implements Serializable {

    private static final long serialVersionUID = -2373609125206297975L;

    // === limiter config identity

    /**
     * The node name or id
     **/
    private String node = LimiterConfig.DEFAULT_NODE;
    /**
     * The application name or id
     **/
    private String application = LimiterConfig.DEFAULT_APPLICATION;

    // === limiter config strategy

    /**
     * Rule properties
     **/
    private List<RuleProperties> rules = new ArrayList<>();

    /**
     * RuleConfig
     *
     * @author lry
     */
    @Data
    public static class RuleProperties implements Serializable {
        /**
         * The group of service resource
         **/
        private String group = LimiterConfig.DEFAULT_GROUP;
        /**
         * The service key or resource key
         **/
        private String tag;
        /**
         * The switch of, default is Switch.ON
         **/
        private Switch enable = Switch.ON;
        /**
         * The model of limiter
         */
        private Mode mode = Mode.STAND_ALONE;
        /**
         * The strategy of limiter, default is Strategy.NON
         */
        private Strategy strategy = Strategy.IGNORE;
        /**
         * The rate limiter
         */
        private RateLimiterConfig rate = new RateLimiterConfig();
        /**
         * The request limiter
         */
        private CounterLimiterConfig request = new CounterLimiterConfig();
        /**
         * The concurrent limiter
         */
        private ConcurrentLimiterConfig concurrent = new ConcurrentLimiterConfig();
    }

}
