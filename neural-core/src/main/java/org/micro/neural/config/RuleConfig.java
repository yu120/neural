package org.micro.neural.config;

import lombok.*;
import org.micro.neural.common.Constants;
import org.micro.neural.config.GlobalConfig.*;

import java.io.Serializable;

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
     * The remarks
     **/
    private String remarks;

    public String identity() {
        return (module + Constants.DELIMITER + application +
                Constants.DELIMITER + group + Constants.DELIMITER + resource).toUpperCase();
    }

}
