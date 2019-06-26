package org.micro.neural.config.event;

/**
 * The Event Type.
 *
 * @author lry
 */
public interface IEventType {

    /**
     * The event type name
     *
     * @return event type name
     */
    String name();

    /**
     * The event type module
     *
     * @return event type module
     */
    String getModule();

    /**
     * The event type message
     *
     * @return event type message
     */
    String getMessage();

}
