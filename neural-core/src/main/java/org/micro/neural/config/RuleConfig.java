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

    private static final long serialVersionUID = 5564143662571971030L;

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
        return application + Constants.DELIMITER + group + Constants.DELIMITER + resource;
    }

}
