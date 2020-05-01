package cn.micro.neural.idempotent.core;

import cn.micro.neural.idempotent.IdempotentConfig;
import cn.micro.neural.idempotent.event.EventListener;
import cn.micro.neural.storage.OriginalCall;
import cn.micro.neural.storage.OriginalContext;
import cn.neural.common.extension.SPI;

import java.util.Map;

/**
 * Idempotent
 *
 * @author lry
 */
@SPI("stand-alone")
public interface Idempotent {

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
     * @throws Exception The Exception is execute refresh configuration
     */
    boolean refresh(IdempotentConfig config) throws Exception;

    /**
     * The collect metric(get and reset)
     *
     * @return key={@link IdempotentConfig#identity()}, subKey=metric key
     */
    Map<String, Long> collect();

    /**
     * The process of original call
     *
     * @param originalContext {@link OriginalContext}
     * @param originalCall    {@link OriginalCall}
     * @return original call return result
     * @throws Throwable throw exception
     */
    Object wrapperCall(final OriginalContext originalContext, final OriginalCall originalCall) throws Throwable;

}
