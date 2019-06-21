package org.micro.neural.metric;

import org.micro.neural.extension.SPI;

import java.util.Map;

@SPI(single = true)
public interface IMetric {

    /**
     * The get metric
     */
    Map<String, Object> getMetric();

}
