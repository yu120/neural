package cn.micro.neural.circuitbreaker.core;

import cn.micro.neural.circuitbreaker.CircuitBreakerConfig;
import cn.micro.neural.circuitbreaker.event.EventListener;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import cn.neural.common.extension.SPI;

import java.util.Map;

/**
 * ICircuitBreaker
 *
 * @author lry
 */
@SPI("stand-alone")
public interface ICircuitBreaker {

    /**
     * The get limiter config
     *
     * @return configuration
     */
    CircuitBreakerConfig getConfig();

    /**
     * The add event listener
     *
     * @param eventListeners event listeners
     */
    void addListener(EventListener... eventListeners);

    /**
     * The refresh in-memory data.
     *
     * @param config configuration
     * @return true is success
     * @throws Exception The Exception is execute refresh LimiterConfig
     */
    boolean refresh(CircuitBreakerConfig config) throws Exception;

    /**
     * The process of original call
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    Object wrapperCall(final OriginalContext originalContext, final OriginalCall originalCall) throws Throwable;

    /**
     * The collect metric(get and reset)
     *
     * @return key=metric key, value=metric value
     */
    Map<String, Long> collect();

}
