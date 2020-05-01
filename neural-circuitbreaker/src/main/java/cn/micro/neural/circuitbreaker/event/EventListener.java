package cn.micro.neural.circuitbreaker.event;

import cn.micro.neural.circuitbreaker.CircuitBreakerConfig;

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
    void onEvent(CircuitBreakerConfig config, EventType eventType, Object... args);

}