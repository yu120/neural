package org.micro.neural.config.event;

import org.micro.neural.extension.SPI;

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
     * @param eventType {@link IEventType}
     * @param object    parameter
     */
    void onEvent(IEventType eventType, Object object);

    /**
     * The destroy store config
     */
    void destroy();

}
