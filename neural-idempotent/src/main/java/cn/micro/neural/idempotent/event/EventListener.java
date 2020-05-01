package cn.micro.neural.idempotent.event;

import cn.micro.neural.idempotent.IdempotentConfig;

/**
 * EventListener
 *
 * @author lry
 */
public interface EventListener {

    /**
     * The notify event
     *
     * @param config    {@link IdempotentConfig}
     * @param eventType {@link EventType}
     * @param args      attachment parameters
     */
    void onEvent(IdempotentConfig config, EventType eventType, Object... args);

}