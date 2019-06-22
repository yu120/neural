package org.micro.neural.config.event;

import org.micro.neural.extension.SPI;

import java.util.Map;

/**
 * The Event Notify.
 *
 * @author lry
 **/
@SPI("log")
public interface IEventListener {

    /**
     * The initialize
     *
     * @param eventConfig {@link EventConfig}
     */
    void initialize(EventConfig eventConfig);

    /**
     * The notify event
     *
     * @param eventType  {@link IEventType}
     * @param parameters parameter list
     */
    void notify(IEventType eventType, Map<String, Object> parameters);

}
