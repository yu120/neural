package cn.micro.neural.circuitbreaker;

import cn.micro.neural.circuitbreaker.exception.CircuitBreakerException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * CircuitBreakerConfig
 * <p>
 * 1.默认为closed状态
 * 2.在closed状态下，时间窗口failCountWindowInMs范围内，连续失败failThreshold次,则打开熔断为open状态
 * 3.在open状态下，经过open2HalfOpenTimeoutInMs长的超时等待后(熔断休眠时间)，状态将自动进入half-open状态中
 * 4.在half-open状态下，连续经过consecutiveSuccessThreshold此成功后，状态自动变为closed状态，否则失败一次则重置计数
 *
 * @author lry
 */
@Data
public class CircuitBreakerConfig implements Serializable {

    public static String DELIMITER = ":";
    public static final String DEFAULT_NODE = "circuit_breaker";
    public static final String DEFAULT_APPLICATION = "micro";
    public static final String DEFAULT_GROUP = "neural";

    // === Circuit-Breaker config identity

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

    // === circuit-breaker config intro

    /**
     * The switch of, default is Switch.ON
     **/
    private Switch enable = Switch.ON;
    /**
     * The circuit-breaker name
     **/
    private String name;
    /**
     * The label list of circuit-breaker
     */
    private List<String> labels = new ArrayList<>();
    /**
     * The circuit-breaker intro
     **/
    private String intro;

    // === circuit-breaker config strategy

    /**
     * The model of circuit-breaker
     */
    private Mode mode = Mode.STAND_ALONE;
    /**
     * closed状态的失败次数阈值
     */
    private int failThreshold = 5;
    /**
     * closed状态的失败计数的时间窗口
     */
    private int failCountWindowInMs = 60 * 1000;
    /**
     * 处于open状态下进入half-open的超时时间
     */
    private int open2HalfOpenTimeoutInMs = 5 * 1000;
    /**
     * half-open状态下成功次数阈值
     */
    private int consecutiveSuccessThreshold = 5;
    /**
     * 排除的异常的ClassName全称
     */
    private List<String> excludeExceptions = new ArrayList<>();
    /**
     * 包含的异常的ClassName全称
     */
    private List<String> includeExceptions = new ArrayList<>();

    /**
     * Config identity key
     *
     * @return identity = {@link CircuitBreakerConfig#getNode()} + {@link CircuitBreakerConfig#DELIMITER}
     * + {@link CircuitBreakerConfig#getApplication()} + {@link CircuitBreakerConfig#DELIMITER}
     * + {@link CircuitBreakerConfig#getGroup()} + {@link CircuitBreakerConfig#DELIMITER}
     * + {@link CircuitBreakerConfig#getTag()}
     */
    public String identity() {
        if (Stream.of(node, application, group, tag).anyMatch(s -> s.contains(DELIMITER))) {
            throw new CircuitBreakerException("The identity key can't include ':'");
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

}
