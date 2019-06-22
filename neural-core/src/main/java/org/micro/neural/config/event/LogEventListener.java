package org.micro.neural.config.event;

import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The Log Event Notify.
 *
 * @author lry
 **/
@Extension("log")
public class LogEventListener implements IEventListener {

    private Logger eventLog;
    private EventConfig eventConfig;

    @Override
    public void initialize(EventConfig eventConfig) {
        this.eventConfig = eventConfig;
        // build event log
        this.eventLog = LoggerFactory.getLogger(eventConfig.getLogName());
    }

    @Override
    public void notify(IEventType eventType, Map<String, Object> parameters) {
        if (eventConfig.isJsonLog()) {
            eventLog.info("{}", SerializeUtils.serialize(parameters));
        } else {
            eventLog.info("{}", parameters.toString());
        }
    }

}
