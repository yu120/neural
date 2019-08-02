package org.micro.neural.config.event;

import org.micro.neural.extension.Extension;

/**
 * The Redis Event Notify.
 *
 * @author lry
 **/
@Extension("redis")
public class RedisEventListener implements IEventListener {

    private EventConfig eventConfig;

    @Override
    public void initialize(EventConfig eventConfig) {
        this.eventConfig = eventConfig;
    }

    @Override
    public void onEvent(IEventType eventType, Object object) {

    }

    @Override
    public void destroy() {

    }

}
