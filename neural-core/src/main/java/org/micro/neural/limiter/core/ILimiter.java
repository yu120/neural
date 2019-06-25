package org.micro.neural.limiter.core;

import org.micro.neural.OriginalCall;
import org.micro.neural.common.URL;
import org.micro.neural.extension.SPI;
import org.micro.neural.limiter.LimiterConfig;
import org.micro.neural.limiter.LimiterGlobalConfig;
import org.micro.neural.limiter.LimiterStatistics;

/**
 * The Limiter Interface.
 *
 * @author lry
 */
@SPI("local")
public interface ILimiter {

    /**
     * The initialize limiter
     *
     * @param limiterGlobalConfig {@link LimiterGlobalConfig}
     */
    void initialize(LimiterGlobalConfig limiterGlobalConfig);

    /**
     * The get config of limiter.
     *
     * @return The LimiterConfig
     */
    LimiterConfig getLimiterConfig();

    /**
     * The refresh in-memory data.
     *
     * @param limiterConfig       The LimiterConfig
     * @return true is success
     * @throws Exception The Exception is execute refresh LimiterConfig
     */
    boolean refresh(LimiterConfig limiterConfig) throws Exception;

    /**
     * The process original call.
     *
     * @param originalCall The Limiter Config
     * @return The object of OriginalCall
     * @throws Throwable The Exception is execute doOriginalCall
     */
    Object doOriginalCall(OriginalCall originalCall) throws Throwable;

    /**
     * The get statistics of limiter.
     *
     * @return The Limiter Statistics
     */
    LimiterStatistics getStatistics();

}
