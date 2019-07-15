package org.micro.neural.bloomfilter;

import lombok.*;
import org.micro.neural.config.GlobalConfig;

/**
 * The Global Config of Limiter.
 *
 * @author lry
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BloomFilterGlobalConfig extends GlobalConfig {

    private static final long serialVersionUID = -9072659813214931506L;

    public static final String IDENTITY = "bloom-filter";

}