package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.event.EventListener;
import cn.neural.common.extension.SPI;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;

import java.util.Map;

/**
 * The Limiter Interface.
 *
 * @author lry
 */
@SPI("stand-alone")
public interface ILimiter {

    /**
     * The get limiter config
     *
     * @return configuration
     */
    LimiterConfig getConfig();

    /**
     * The add event listener
     *
     * @param eventListeners {@link EventListener}
     */
    void addListener(EventListener... eventListeners);

    /**
     * The refresh in-memory data.
     *
     * @param limiterConfig The LimiterConfig
     * @return true is success
     * @throws Exception The Exception is execute refresh LimiterConfig
     */
    boolean refresh(LimiterConfig limiterConfig) throws Exception;

    /**
     * The process original call.
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    {@link OriginalCall}
     * @return The object of OriginalCall
     * @throws Throwable The Exception is execute doOriginalCall
     */
    Object wrapperCall(final OriginalContext originalContext, final OriginalCall originalCall) throws Throwable;

    /**
     * The collect metric(get and reset)
     *
     * @return key=metric key, value=metric value
     */
    Map<String, Long> collect();

}
