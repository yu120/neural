package cn.micro.neural.limiter.core;

import cn.micro.neural.limiter.LimiterConfig;
import cn.micro.neural.limiter.LimiterStatistics;
import cn.micro.neural.limiter.LimiterContext;
import cn.micro.neural.limiter.OriginalCall;
import cn.neural.common.extension.SPI;

/**
 * The Limiter Interface.
 *
 * @author lry
 */
@SPI("local")
public interface ILimiter {

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
     * @param originalCall  {@link OriginalCall}
     * @return The object of OriginalCall
     * @throws Throwable The Exception is execute doOriginalCall
     */
    Object wrapperCall(LimiterContext limiterContext, OriginalCall originalCall) throws Throwable;

    /**
     * The get statistics of limiter.
     *
     * @return The Limiter Statistics
     */
    LimiterStatistics getStatistics();

}
