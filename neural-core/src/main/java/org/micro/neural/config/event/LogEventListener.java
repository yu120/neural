package org.micro.neural.config.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.micro.neural.common.utils.SerializeUtils;
import org.micro.neural.extension.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void notify(IEventType eventType, Object... args) {
        List<Object> argList = new ArrayList<>();
        if (args != null && args.length > 0) {
            argList = Arrays.asList(args);
        }

        EventCollect eventCollect = new EventCollect(eventType.getModule(), eventType.name(), argList);
        if (eventConfig.isJsonLog()) {
            eventLog.info("{}", SerializeUtils.serialize(eventCollect));
        } else {
            eventLog.info("{}", eventCollect.toString());
        }
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventCollect implements Serializable {
        private String module;
        private String eventType;
        private List<Object> args;
    }

}
