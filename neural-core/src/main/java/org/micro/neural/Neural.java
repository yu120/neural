package org.micro.neural;

import org.micro.neural.common.URL;
import org.micro.neural.config.GlobalConfig;
import org.micro.neural.config.RuleConfig;

import java.util.Map;

/**
 * The Neural.
 *
 * @author lry
 */
public interface Neural<C extends RuleConfig, G extends GlobalConfig> {

    /**
     * The initialize
     *
     * @param url {@link URL}
     */
    void initialize(URL url);

    /**
     * The get global config
     *
     * @return {@link G}
     */
    G getGlobalConfig();

    /**
     * The add degrade
     *
     * @param config {@link C}
     */
    void addConfig(C config);

    /**
     * The process of original call
     *
     * @param identity     {@link org.micro.neural.config.RuleConfig}
     * @param originalCall {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    Object originalCall(String identity, OriginalCall originalCall) throws Throwable;

    /**
     * The process of original call
     *
     * @param neuralContext {@link NeuralContext}
     * @param identity      {@link org.micro.neural.config.RuleConfig}
     * @param originalCall  {@link OriginalCall}
     * @return invoke return object
     * @throws Throwable throw exception
     */
    Object originalCall(NeuralContext neuralContext, String identity, OriginalCall originalCall) throws Throwable;

    /**
     * The notify of changed config
     *
     * @param category {@link GlobalConfig.Category}
     * @param identity the config identity, format: [module]:[application]:[group]:[resource]
     * @param data     the config data, format: serialize config data
     */
    void notify(GlobalConfig.Category category, String identity, String data);

    /**
     * The collect of get and reset statistics data
     *
     * @return statistics data
     */
    Map<String, Map<String, Long>> collect();

    /**
     * The get statistics data
     *
     * @return statistics data
     */
    Map<String, Map<String, Long>> statistics();

    /**
     * The destroy
     */
    void destroy();

}
