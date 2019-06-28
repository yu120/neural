package org.micro.neural.config;

import lombok.*;
import org.micro.neural.common.Constants;
import org.micro.neural.config.GlobalConfig.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Neural of Config.
 *
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class RuleConfig implements Serializable {

    private static final long serialVersionUID = 1587739377558585912L;

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
        if (module.contains(Constants.DELIMITER) ||
                application.contains(Constants.DELIMITER) ||
                group.contains(Constants.DELIMITER) ||
                resource.contains(Constants.DELIMITER)) {
            throw new IllegalArgumentException("The identity key can't include ':'");
        }

        return (module + Constants.DELIMITER + application +
                Constants.DELIMITER + group + Constants.DELIMITER + resource).toUpperCase();
    }

}
