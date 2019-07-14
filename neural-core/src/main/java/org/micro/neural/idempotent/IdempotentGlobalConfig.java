package org.micro.neural.idempotent;

import lombok.*;
import org.micro.neural.config.GlobalConfig;

/**
 * The Global Config of Idempotent.
 *
 * @author lry
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class IdempotentGlobalConfig extends GlobalConfig {

    private static final long serialVersionUID = -9072659813214931506L;

    public static final String IDENTITY = "idempotent";
    /**
     * The timeout
     */
    private Long timeout = 0L;

}