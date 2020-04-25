package cn.micro.neural.limiter;

/**
 * EventListener
 *
 * @author lry
 */
public interface EventListener {

    /**
     * The notify event
     *
     * @param limiterConfig {@link LimiterConfig}
     * @param eventType     {@link EventType}
     * @param args          attachment parameters
     */
    void onEvent(LimiterConfig limiterConfig, EventType eventType, Object... args);

}