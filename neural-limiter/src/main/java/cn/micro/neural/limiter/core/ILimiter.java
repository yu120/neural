package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterContext;
import cn.micro.neural.limiter.OriginalCall;
import cn.micro.neural.limiter.event.EventListener;
import cn.neural.common.extension.SPI;

import java.util.Map;

/**
 * The Limiter Interface.
 *
 * @author lry
 */
@SPI("local")
public interface ILimiter {

    /**
     * The get limiter config
     *
     * @return {@link LimiterConfig}
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
     * @param limiterContext {@link LimiterContext}
     * @param originalCall   {@link OriginalCall}
     * @return The object of OriginalCall
     * @throws Throwable The Exception is execute doOriginalCall
     */
    Object wrapperCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable;

    /**
     * The collect metric(get and reset)
     *
     * @return key={@link LimiterConfig#identity()}, subKey=metric key
     */
    Map<String, Long> collect();

    /**
     * The statistics metric(get)
     *
     * @return key={@link LimiterConfig#identity()}, subKey=metric key
     */
    Map<String, Long> statistics();

}
