package cn.micro.neural.idempotent;

import cn.micro.neural.idempotent.exception.IdempotentException;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * IdempotentConfig
 *
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
public class IdempotentConfig implements Serializable {

    public static String DELIMITER = ":";
    public static final String DEFAULT_NODE = "idempotent";
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
     * Config identity key
     */
    public String identity() {
        if (Stream.of(node, application, group, tag).anyMatch(s -> s.contains(DELIMITER))) {
            throw new IdempotentException("The identity key can't include ':'");
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
