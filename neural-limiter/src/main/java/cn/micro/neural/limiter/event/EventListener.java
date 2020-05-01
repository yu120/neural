package cn.micro.neural.limiter.event;

import cn.micro.neural.limiter.LimiterConfig;

/**
 * EventListener
 *
 * @author lry
 */
public interface EventListener {

    /**
     * The notify event
     *
     * @param config    configuration
     * @param eventType event type
     * @param args      attachment parameters
     */
    void onEvent(LimiterConfig config, EventType eventType, Object... args);

}