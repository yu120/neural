package org.micro.neural;

import org.micro.neural.config.GlobalConfig;
import org.micro.neural.config.RuleConfig;
import org.micro.neural.extension.SPI;

/**
 * The Endpoint Neural
 *
 * @param <C> extends {@link RuleConfig}
 * @param <G> extends {@link GlobalConfig}
 * @author lry
 **/
@SPI
public interface EndpointNeural<C extends RuleConfig, G extends GlobalConfig> extends Neural<C, G> {

}
