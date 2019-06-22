package org.micro.neural.config.event;

import org.micro.neural.config.store.StorePool;
import org.micro.neural.extension.Extension;

import java.util.Map;

/**
 * The Redis Event Notify.
 *
 * @author lry
 **/
@Extension("redis")
public class RedisEventListener implements IEventListener {

    private StorePool storePool = StorePool.getInstance();

    private EventConfig eventConfig;

    @Override
    public void initialize(EventConfig eventConfig) {
        this.eventConfig = eventConfig;
    }

    @Override
    public void notify(IEventType eventType, Map<String, Object> parameters) {

    }

}
