package org.micro.neural.bloomfilter;

import lombok.*;
import org.micro.neural.config.RuleConfig;

/**
 * The Bloom Filter Config.
 *
 * @author lry
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BloomFilterConfig extends RuleConfig {

    private static final long serialVersionUID = 4076904823256002967L;

    /**
     * The model of limiter
     */
    private String model = "stand-alone";

    // === Bloom Filter limiter

    /**
     * The false positive probability
     */
    private double falsePositiveProbability = 0.0001;
    /**
     * The expected number of elements
     */
    private Integer expectedNumberOfElements = 10000;

}
